package com.beiktech.bontolink.tool.synonym;

import com.alibaba.fastjson2.JSON;
import com.beiktech.bontolink.base.embedding.EmbeddingService;
import com.beiktech.bontolink.base.llm.SynonymClient;
import io.milvus.client.MilvusServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class EntitySynonymService {

    // ── 批量进度（轮询用） ──────────────────────────────────────────────────────
    private volatile String batchStatus = "idle";
    private volatile String batchEntityType = "";
    private volatile int batchTotal = 0;
    private final AtomicInteger batchProcessed = new AtomicInteger(0);
    private final AtomicInteger batchFailed = new AtomicInteger(0);

    public Map<String, Object> getBatchProgress() {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("status", batchStatus);
        p.put("entityType", batchEntityType);
        p.put("total", batchTotal);
        p.put("processed", batchProcessed.get());
        p.put("failed", batchFailed.get());
        return p;
    }

    @Autowired
    private SynonymClient synonymClient;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private ApplicationContext ctx;

    @Autowired(required = false)
    private MilvusServiceClient milvusClient;

    @Value("${bontolink.ontology.vector.milvus-collection:ont_entity_embeddings}")
    private String collection;

    /** 每次调用时从 context 获取最新的 JdbcTemplate，避免 JRebel 热加载后旧连接池问题 */
    private JdbcTemplate db() {
        return ctx.getBean(JdbcTemplate.class);
    }

    // ── 单条生成 ────────────────────────────────────────────────────────────

    public Map<String, Object> generate(String entityType, String entityId) {
        EntityInfo info = fetchEntityInfo(entityType, entityId);
        if (info == null) {
            return Map.of("success", false, "message", "实体不存在: " + entityType + "/" + entityId);
        }

        List<String> synonyms = synonymClient.generateSynonyms(info.word, info.desc);
        saveSynonyms(entityType, entityId, info.word, synonyms, info.nsCode);
        if (milvusClient != null) {
            upsertToMilvus(entityType, entityId, info, synonyms);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("entityType", entityType);
        result.put("entityId", entityId);
        result.put("word", info.word);
        result.put("synonyms", synonyms);
        result.put("milvusUpserted", milvusClient != null);
        return result;
    }

    // ── 批量生成 ────────────────────────────────────────────────────────────

    public Map<String, Object> batchGenerate(String entityType, int limit) {
        List<Map<String, Object>> entities = fetchEntityList(entityType, limit);
        batchStatus = "running";
        batchEntityType = entityType;
        batchTotal = entities.size();
        batchProcessed.set(0);
        batchFailed.set(0);
        try {
            for (Map<String, Object> row : entities) {
                String id = str(row, "id");
                try {
                    generate(entityType, id);
                    batchProcessed.incrementAndGet();
                } catch (Exception e) {
                    log.warn("批量生成失败: type={} id={} err={}", entityType, id, e.getMessage(), e);
                    batchProcessed.incrementAndGet();
                    batchFailed.incrementAndGet();
                }
            }
        } finally {
            batchStatus = "idle";
        }
        return Map.of("success", true, "entityType", entityType,
                "total", entities.size(), "succeeded", batchProcessed.get() - batchFailed.get(),
                "failed", batchFailed.get());
    }

    // ── DB同义词 → Milvus 同步（不重跑LLM） ──────────────────────────────────

    public Map<String, Object> syncFromDict(String entityType, int limit) {
        if (milvusClient == null) {
            return Map.of("success", false, "message", "Milvus 未启用");
        }
        // 按实体聚合：dict 表一个实体有多条同义词记录，每实体只处理一次
        String sql = "SELECT entity_type, entity_id, word FROM sys_synonym_dict";
        List<Object> params = new ArrayList<>();
        if (entityType != null && !entityType.isBlank()) {
            sql += " WHERE entity_type = ?";
            params.add(entityType);
        }
        sql += " ORDER BY entity_type, entity_id, word";
        if (limit > 0) sql += " LIMIT " + limit;

        List<Map<String, Object>> rows = params.isEmpty()
                ? db().queryForList(sql)
                : db().queryForList(sql, params.toArray());

        // Java 端聚合：entity_id → 全部同义词列表
        Map<String, List<String>> wordMap = new LinkedHashMap<>();
        Map<String, String> typeMap = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String eType = str(row, "entity_type");
            String eId = str(row, "entity_id");
            String word = str(row, "word");
            if (eId == null) continue;
            typeMap.put(eId, eType);
            wordMap.computeIfAbsent(eId, k -> new ArrayList<>()).add(word);
        }

        batchStatus = "running";
        batchEntityType = entityType != null ? entityType : "all";
        batchTotal = wordMap.size();
        batchProcessed.set(0);
        batchFailed.set(0);
        int sinceFlush = 0;
        try {
            for (Map.Entry<String, List<String>> entry : wordMap.entrySet()) {
                String eId = entry.getKey();
                String eType = typeMap.get(eId);
                List<String> synonyms = entry.getValue();
                try {
                    EntityInfo info = fetchEntityInfo(eType, eId);
                    if (info == null) {
                        // 实体已删除，用同义词第一个兜底
                        info = new EntityInfo(synonyms.get(0), null, null, null, null);
                    }
                    upsertToMilvus(eType, eId, info, synonyms);
                    batchProcessed.incrementAndGet();
                    // 周期性强制落盘，防止 buffer 堆积导致变慢/不可见
                    if (++sinceFlush >= MILVUS_FLUSH_INTERVAL) {
                        sinceFlush = 0;
                        flushMilvus();
                    }
                } catch (Exception e) {
                    log.warn("同步Milvus失败: type={} id={} err={}", eType, eId, e.getMessage());
                    batchProcessed.incrementAndGet();
                    batchFailed.incrementAndGet();
                }
            }
            // 收尾兜底 flush
            flushMilvus();
        } finally {
            batchStatus = "idle";
        }
        return Map.of("success", true, "total", wordMap.size(),
                "succeeded", batchProcessed.get() - batchFailed.get(), "failed", batchFailed.get());
    }

    // ── 实体信息查询 ─────────────────────────────────────────────────────────

    private EntityInfo fetchEntityInfo(String entityType, String entityId) {
        return switch (entityType) {
            case "class" -> fetchClass(entityId);
            case "class_prop" -> fetchClassProp(entityId);
            case "link_type" -> fetchLinkType(entityId);
            case "shared_prop" -> fetchSharedProp(entityId);
            case "biz_category" -> fetchBizCategory(entityId);
            case "enum_item" -> fetchEnumItem(entityId);
            default -> null;
        };
    }

    private List<Map<String, Object>> fetchEntityList(String entityType, int limit) {
        String sql = switch (entityType) {
            case "class" -> "SELECT id FROM ont_class ORDER BY create_time DESC";
            case "class_prop" -> "SELECT id FROM ont_class_property ORDER BY create_time DESC";
            case "link_type" -> "SELECT id FROM ont_link_types ORDER BY created_at DESC";
            case "shared_prop" -> "SELECT id FROM ont_shared_properties ORDER BY create_time DESC";
            case "biz_category" -> "SELECT id FROM ont_biz_category ORDER BY create_time DESC";
            case "enum_item" -> "SELECT id FROM ont_enum_items ORDER BY create_time DESC";
            default -> throw new IllegalArgumentException("未知 entityType: " + entityType);
        };
        if (limit > 0) sql += " LIMIT " + limit;
        return db().queryForList(sql);
    }

    private EntityInfo fetchClass(String id) {
        List<Map<String, Object>> rows = db().queryForList(
                "SELECT c.id, c.api_name, c.display_name, c.rdfs_label, c.rdfs_comment, c.description," +
                " b.ns_code FROM ont_class c LEFT JOIN ont_biz_category b ON c.category_code = b.category_code" +
                " WHERE c.id = ?", id);
        if (rows.isEmpty()) return null;
        Map<String, Object> r = rows.get(0);
        String word = coalesce(r, "rdfs_label", "display_name", "api_name");
        String desc = join(" ", str(r, "rdfs_comment"), str(r, "description"));
        return new EntityInfo(word, desc, str(r, "ns_code"), null, str(r, "api_name"));
    }

    private EntityInfo fetchClassProp(String id) {
        List<Map<String, Object>> rows = db().queryForList(
                "SELECT p.id, p.api_name AS prop_api, p.display_name, p.rdfs_label, p.rdfs_comment, p.class_id," +
                " c.api_name AS class_api, b.ns_code FROM ont_class_property p" +
                " LEFT JOIN ont_class c ON p.class_id = c.id" +
                " LEFT JOIN ont_biz_category b ON c.category_code = b.category_code" +
                " WHERE p.id = ?", id);
        if (rows.isEmpty()) return null;
        Map<String, Object> r = rows.get(0);
        String word = coalesce(r, "rdfs_label", "display_name", "prop_api");
        String desc = str(r, "rdfs_comment");
        String classApi = str(r, "class_api");
        String propApi = str(r, "prop_api");
        String apiName = (classApi != null && propApi != null) ? classApi + "." + propApi : propApi;
        return new EntityInfo(word, desc, str(r, "ns_code"), str(r, "class_id"), apiName);
    }

    private EntityInfo fetchLinkType(String id) {
        List<Map<String, Object>> rows = db().queryForList(
                "SELECT l.id, l.rdfs_label, l.rdfs_comment, l.l_display_name, l.r_display_name," +
                " l.l_api_name, l.r_api_name," +
                " b.ns_code FROM ont_link_types l" +
                " LEFT JOIN ont_biz_category b ON l.category_code = b.category_code" +
                " WHERE l.id = ?", id);
        if (rows.isEmpty()) return null;
        Map<String, Object> r = rows.get(0);
        String word = coalesce(r, "rdfs_label", "l_display_name", "r_display_name", "id");
        String desc = join(" ", str(r, "rdfs_comment"), str(r, "l_display_name"), str(r, "r_display_name"));
        String apiName = coalesce(r, "l_api_name", "r_api_name", "id");
        return new EntityInfo(word, desc, str(r, "ns_code"), null, apiName);
    }

    private EntityInfo fetchSharedProp(String id) {
        List<Map<String, Object>> rows = db().queryForList(
                "SELECT p.id, p.api_name, p.prop_code, p.rdfs_label, p.rdfs_comment," +
                " b.ns_code FROM ont_shared_properties p" +
                " LEFT JOIN ont_biz_category b ON p.category_code = b.category_code" +
                " WHERE p.id = ?", id);
        if (rows.isEmpty()) return null;
        Map<String, Object> r = rows.get(0);
        String word = coalesce(r, "rdfs_label", "prop_code");
        String desc = str(r, "rdfs_comment");
        String apiName = coalesce(r, "api_name", "prop_code");
        return new EntityInfo(word, desc, str(r, "ns_code"), null, apiName);
    }

    private EntityInfo fetchBizCategory(String id) {
        List<Map<String, Object>> rows = db().queryForList(
                "SELECT id, category_code, rdfs_label, rdfs_comment, description, ns_code, parent_id" +
                " FROM ont_biz_category WHERE id = ?", id);
        if (rows.isEmpty()) return null;
        Map<String, Object> r = rows.get(0);
        String word = coalesce(r, "rdfs_label", "category_code");
        String desc = join(" ", str(r, "rdfs_comment"), str(r, "description"));
        return new EntityInfo(word, desc, str(r, "ns_code"), str(r, "parent_id"), str(r, "category_code"));
    }

    private EntityInfo fetchEnumItem(String id) {
        List<Map<String, Object>> rows = db().queryForList(
                "SELECT ei.id, ei.code, ei.api_name, ei.label, ei.enum_id," +
                " et.rdfs_label AS enum_label, et.api_name AS enum_api_name, b.ns_code" +
                " FROM ont_enum_items ei" +
                " LEFT JOIN ont_enum_types et ON ei.enum_id = et.id" +
                " LEFT JOIN ont_biz_category b ON et.category_code = b.category_code" +
                " WHERE ei.id = ?", id);
        if (rows.isEmpty()) return null;
        Map<String, Object> r = rows.get(0);
        String word = coalesce(r, "label", "api_name", "code");
        String enumLabel = str(r, "enum_label");
        String desc = enumLabel != null ? enumLabel + " - " + word : word;
        // enum_item 向量用父枚举的 api_name，与 Jena 检索对齐
        return new EntityInfo(word, desc, str(r, "ns_code"), str(r, "enum_id"), str(r, "enum_api_name"));
    }

    // ── 同义词写库 ────────────────────────────────────────────────────────────

    private void saveSynonyms(String entityType, String entityId, String word,
                              List<String> synonyms, String nsCode) {
        if (synonyms.isEmpty()) return;
        String domain = nsCode != null ? nsCode : "default";
        java.sql.Timestamp now = java.sql.Timestamp.valueOf(LocalDateTime.now());

        // 先删旧记录，再批量插入 —— 兼容 SQLite 和 PostgreSQL
        db().update(
                "DELETE FROM sys_synonym_dict WHERE entity_type=? AND entity_id=?",
                entityType, entityId);

        List<Object[]> rows = new ArrayList<>();
        for (String synonym : synonyms) {
            String id = "syn-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            rows.add(new Object[]{ id, synonym, JSON.toJSONString(List.of(synonym)),
                    domain, 0.9, "AI", 0, now, now, entityType, entityId });
        }

        try {
            db().batchUpdate(
                    "INSERT INTO sys_synonym_dict" +
                    " (id, word, synonyms, domain, confidence, source, usage_count," +
                    " create_time, update_time, entity_type, entity_id)" +
                    " VALUES (?,?,?,?,?,?,?,?,?,?,?)" +
                    " ON CONFLICT (entity_type, entity_id, word) DO UPDATE SET" +
                    " synonyms=EXCLUDED.synonyms, domain=EXCLUDED.domain," +
                    " confidence=EXCLUDED.confidence, update_time=EXCLUDED.update_time",
                    rows);
            log.info("同义词已写库: entityType={} entityId={} word={} count={}", entityType, entityId, word, synonyms.size());
        } catch (Exception e) {
            log.error("同义词写库失败: entityType={} entityId={} word={} sqlParams={}",
                    entityType, entityId, word, JSON.toJSONString(rows.get(0)), e);
            throw e;
        }
    }

    // ── Milvus insert ─────────────────────────────────────────────────────────

    private void upsertToMilvus(String entityType, String entityId, EntityInfo info, List<String> synonyms) {
        try {
            // 方案A：主词 + 每个同义词各生成 1 条独立向量（pk 带序号），
            // 查询按 entity_id 去重取最高分，避免拼接长文本稀释单个同义词的语义。
            List<String> texts = new ArrayList<>();
            if (info.word != null && !info.word.isBlank()) texts.add(info.word);
            if (synonyms != null) {
                for (String s : synonyms) {
                    if (s != null && !s.isBlank() && !texts.contains(s)) texts.add(s);
                }
            }
            if (texts.isEmpty()) {
                log.warn("Milvus insert 跳过: 无文本 pk={}", entityType + ":" + entityId);
                return;
            }

            // 一次批量嵌入该实体的所有词条（接口为批量语义，底层实现是否并发由 provider 决定）
            List<float[]> vecs = embeddingService.embedBatch(texts);

            List<String> pks = new ArrayList<>();
            List<String> types = new ArrayList<>();
            List<String> ids = new ArrayList<>();
            List<String> parents = new ArrayList<>();
            List<String> nsCodes = new ArrayList<>();
            List<String> sources = new ArrayList<>();
            List<String> apiNames = new ArrayList<>();
            List<List<Float>> embeddings = new ArrayList<>();

            for (int i = 0; i < texts.size(); i++) {
                float[] vec = vecs.get(i);
                if (vec == null || vec.length == 0) continue;
                String text = texts.get(i);
                pks.add(entityType + ":" + entityId + ":" + i);
                types.add(entityType);
                ids.add(entityId);
                parents.add(info.parentId() != null ? info.parentId() : "");
                nsCodes.add(info.nsCode() != null ? info.nsCode() : "");
                sources.add(text.length() > 2048 ? text.substring(0, 2048) : text);
                apiNames.add(info.apiName() != null ? info.apiName() : "");
                embeddings.add(toFloatList(vec));
            }
            if (pks.isEmpty()) {
                log.warn("Milvus insert 跳过: 向量全空 pk={}", entityType + ":" + entityId);
                return;
            }

            io.milvus.param.dml.InsertParam param = io.milvus.param.dml.InsertParam.newBuilder()
                    .withCollectionName(collection)
                    .withFields(List.of(
                            new io.milvus.param.dml.InsertParam.Field("pk",           pks),
                            new io.milvus.param.dml.InsertParam.Field("entity_type",  types),
                            new io.milvus.param.dml.InsertParam.Field("entity_id",    ids),
                            new io.milvus.param.dml.InsertParam.Field("parent_id",    parents),
                            new io.milvus.param.dml.InsertParam.Field("ns_code",      nsCodes),
                            new io.milvus.param.dml.InsertParam.Field("source_text",  sources),
                            new io.milvus.param.dml.InsertParam.Field("api_name",     apiNames),
                            new io.milvus.param.dml.InsertParam.Field("embedding",    embeddings)
                    ))
                    .build();

            milvusClient.insert(param);
            log.info("Milvus insert 完成: pk={} 词条数={} dim={}", entityType + ":" + entityId, pks.size(), vecs.get(0).length);
        } catch (Exception e) {
            log.warn("Milvus insert 失败: entityType={} entityId={} err={}",
                    entityType, entityId, e.getMessage());
        }
    }

    // ── Milvus flush（强制 buffer 落盘） ────────────────────────────────────────

    /** 批量同步中每隔多少条强制 flush 一次，防止 buffer 无限堆积导致变慢/不可见 */
    private static final int MILVUS_FLUSH_INTERVAL = 200;

    private void flushMilvus() {
        if (milvusClient == null) return;
        try {
            milvusClient.flush(
                    io.milvus.param.collection.FlushParam.newBuilder()
                            .withCollectionNames(java.util.List.of(collection))
                            .build());
            log.info("Milvus flush 完成，buffer 已落盘");
        } catch (Exception e) {
            log.warn("Milvus flush 失败: {}", e.getMessage());
        }
    }

    // ── 工具方法 ──────────────────────────────────────────────────────────────

    private static List<Float> toFloatList(float[] arr) {
        List<Float> list = new ArrayList<>(arr.length);
        for (float f : arr) list.add(f);
        return list;
    }

    private static String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? null : v.toString().trim().isEmpty() ? null : v.toString().trim();
    }

    private static String coalesce(Map<String, Object> m, String... keys) {
        for (String k : keys) {
            String v = str(m, k);
            if (v != null) return v;
        }
        return "unknown";
    }

    private static String join(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p != null && !p.isBlank()) {
                if (sb.length() > 0) sb.append(sep);
                sb.append(p);
            }
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    // ── 内部 DTO ──────────────────────────────────────────────────────────────

    private record EntityInfo(String word, String desc, String nsCode, String parentId, String apiName) {}
}
