package com.beiktech.bontolink.tool.synonym;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
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

@Slf4j
@Service
public class EntitySynonymService {

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
        int success = 0, fail = 0;
        for (Map<String, Object> row : entities) {
            String id = str(row, "id");
            try {
                generate(entityType, id);
                success++;
            } catch (Exception e) {
                log.warn("批量生成失败: type={} id={} err={}", entityType, id, e.getMessage(), e);
                fail++;
            }
        }
        return Map.of("success", true, "entityType", entityType,
                "total", entities.size(), "succeeded", success, "failed", fail);
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
            case "link_type" -> "SELECT id FROM ont_link_types ORDER BY create_time DESC";
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
        return new EntityInfo(word, desc, str(r, "ns_code"), null);
    }

    private EntityInfo fetchClassProp(String id) {
        List<Map<String, Object>> rows = db().queryForList(
                "SELECT p.id, p.api_name, p.display_name, p.rdfs_label, p.rdfs_comment, p.class_id," +
                " b.ns_code FROM ont_class_property p" +
                " LEFT JOIN ont_class c ON p.class_id = c.id" +
                " LEFT JOIN ont_biz_category b ON c.category_code = b.category_code" +
                " WHERE p.id = ?", id);
        if (rows.isEmpty()) return null;
        Map<String, Object> r = rows.get(0);
        String word = coalesce(r, "rdfs_label", "display_name", "api_name");
        String desc = str(r, "rdfs_comment");
        return new EntityInfo(word, desc, str(r, "ns_code"), str(r, "class_id"));
    }

    private EntityInfo fetchLinkType(String id) {
        List<Map<String, Object>> rows = db().queryForList(
                "SELECT l.id, l.rdfs_label, l.rdfs_comment, l.l_display_name, l.r_display_name," +
                " b.ns_code FROM ont_link_types l" +
                " LEFT JOIN ont_biz_category b ON l.category_code = b.category_code" +
                " WHERE l.id = ?", id);
        if (rows.isEmpty()) return null;
        Map<String, Object> r = rows.get(0);
        String word = coalesce(r, "rdfs_label", "l_display_name");
        String desc = join(" ", str(r, "rdfs_comment"), str(r, "r_display_name"));
        return new EntityInfo(word, desc, str(r, "ns_code"), null);
    }

    private EntityInfo fetchSharedProp(String id) {
        List<Map<String, Object>> rows = db().queryForList(
                "SELECT p.id, p.prop_code, p.rdfs_label, p.rdfs_comment," +
                " b.ns_code FROM ont_shared_properties p" +
                " LEFT JOIN ont_biz_category b ON p.category_code = b.category_code" +
                " WHERE p.id = ?", id);
        if (rows.isEmpty()) return null;
        Map<String, Object> r = rows.get(0);
        String word = coalesce(r, "rdfs_label", "prop_code");
        String desc = str(r, "rdfs_comment");
        return new EntityInfo(word, desc, str(r, "ns_code"), null);
    }

    private EntityInfo fetchBizCategory(String id) {
        List<Map<String, Object>> rows = db().queryForList(
                "SELECT id, category_code, rdfs_label, rdfs_comment, description, ns_code, parent_id" +
                " FROM ont_biz_category WHERE id = ?", id);
        if (rows.isEmpty()) return null;
        Map<String, Object> r = rows.get(0);
        String word = coalesce(r, "rdfs_label", "category_code");
        String desc = join(" ", str(r, "rdfs_comment"), str(r, "description"));
        return new EntityInfo(word, desc, str(r, "ns_code"), str(r, "parent_id"));
    }

    private EntityInfo fetchEnumItem(String id) {
        List<Map<String, Object>> rows = db().queryForList(
                "SELECT ei.id, ei.code, ei.api_name, ei.label, ei.enum_id," +
                " et.rdfs_label AS enum_label, b.ns_code" +
                " FROM ont_enum_items ei" +
                " LEFT JOIN ont_enum_types et ON ei.enum_id = et.id" +
                " LEFT JOIN ont_biz_category b ON et.category_code = b.category_code" +
                " WHERE ei.id = ?", id);
        if (rows.isEmpty()) return null;
        Map<String, Object> r = rows.get(0);
        String word = coalesce(r, "label", "api_name", "code");
        String enumLabel = str(r, "enum_label");
        String desc = enumLabel != null ? enumLabel + " - " + word : word;
        return new EntityInfo(word, desc, str(r, "ns_code"), str(r, "enum_id"));
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
            String pk = entityType + ":" + entityId;
            String sourceText = info.word + (synonyms.isEmpty() ? "" : " " + String.join(" ", synonyms));
            float[] vec = embeddingService.embed(sourceText);

            if (vec == null || vec.length == 0) {
                log.warn("Milvus insert 跳过: 向量为空 pk={}", pk);
                return;
            }

            List<String> pks = List.of(pk);
            List<String> types = List.of(entityType);
            List<String> ids = List.of(entityId);
            List<String> parents = List.of(info.parentId != null ? info.parentId : "");
            List<String> nsCodes = List.of(info.nsCode != null ? info.nsCode : "");
            List<String> sources = List.of(sourceText.length() > 2048 ? sourceText.substring(0, 2048) : sourceText);
            List<List<Float>> embeddings = List.of(toFloatList(vec));

            // 直接插入（允许重复 pk，Milvus 会保留最新的）
            io.milvus.param.dml.InsertParam param = io.milvus.param.dml.InsertParam.newBuilder()
                    .withCollectionName(collection)
                    .withFields(List.of(
                            new io.milvus.param.dml.InsertParam.Field("pk",           pks),
                            new io.milvus.param.dml.InsertParam.Field("entity_type",  types),
                            new io.milvus.param.dml.InsertParam.Field("entity_id",    ids),
                            new io.milvus.param.dml.InsertParam.Field("parent_id",    parents),
                            new io.milvus.param.dml.InsertParam.Field("ns_code",      nsCodes),
                            new io.milvus.param.dml.InsertParam.Field("source_text",  sources),
                            new io.milvus.param.dml.InsertParam.Field("embedding",    embeddings)
                    ))
                    .build();

            milvusClient.insert(param);
            log.info("Milvus insert 完成: pk={} dim={}", pk, vec.length);
        } catch (Exception e) {
            log.warn("Milvus insert 失败: entityType={} entityId={} err={}",
                    entityType, entityId, e.getMessage());
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

    private record EntityInfo(String word, String desc, String nsCode, String parentId) {}
}
