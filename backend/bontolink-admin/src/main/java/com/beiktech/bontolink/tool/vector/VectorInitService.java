package com.beiktech.bontolink.tool.vector;

import com.beiktech.bontolink.base.embedding.EmbeddingService;
import com.beiktech.bontolink.base.vector.MilvusConfig;
import com.beiktech.bontolink.ontology.config.OntologyEngineConfig;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.collection.GetCollectionStatisticsParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 向量初始化服务 — 从数据库同步实体向量到 Milvus
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "bontolink.ontology.vector.type", havingValue = "milvus")
public class VectorInitService {

    private final EmbeddingService embeddingService;
    private final OntologyEngineConfig engineConfig;
    private final JdbcTemplate jdbcTemplate;
    private final MilvusConfig milvusConfig;

    public VectorInitService(EmbeddingService embeddingService,
                             OntologyEngineConfig engineConfig,
                             JdbcTemplate jdbcTemplate,
                             MilvusConfig milvusConfig) {
        this.embeddingService = embeddingService;
        this.engineConfig = engineConfig;
        this.jdbcTemplate = jdbcTemplate;
        this.milvusConfig = milvusConfig;
    }

    /** 当前 Milvus 客户端（连接自愈后自动取到新实例） */
    private MilvusServiceClient milvusClient() {
        return milvusConfig.current();
    }

    /**
     * 全量同步：从 ont_class 读取所有类，生成向量并插入 Milvus
     */
    public Map<String, Object> syncAll() {
        MilvusServiceClient mc = milvusClient();
        if (mc == null || milvusConfig == null) {
            return error("Milvus 客户端未初始化，请检查配置");
        }

        String collectionName = milvusConfig.getCollectionName();
        log.info("开始全量同步向量到 Milvus collection: {}", collectionName);

        try {
            // 1. 读取所有类
            String sql = """
                SELECT id, api_name, display_name, ns_code, category_code, description
                FROM ont_class
                WHERE status = 1
                ORDER BY ns_code, display_name
                """;
            List<Map<String, Object>> classes = jdbcTemplate.queryForList(sql);

            if (classes.isEmpty()) {
                return success("数据库中没有启用的对象类型，无需同步");
            }

            log.info("从数据库读取 {} 个对象类型", classes.size());

            // 2. 批量生成向量并插入
            int batchSize = 100;
            AtomicInteger total = new AtomicInteger(0);
            AtomicInteger success = new AtomicInteger(0);
            AtomicInteger failed = new AtomicInteger(0);

            List<String> pks = new ArrayList<>(batchSize);
            List<String> entityTypes = new ArrayList<>(batchSize);
            List<String> entityIds = new ArrayList<>(batchSize);
            List<String> parentIds = new ArrayList<>(batchSize);
            List<String> nsCodes = new ArrayList<>(batchSize);
            List<String> sourceTexts = new ArrayList<>(batchSize);
            List<String> apiNames = new ArrayList<>(batchSize);
            List<List<Float>> embeddings = new ArrayList<>(batchSize);

            for (Map<String, Object> cls : classes) {
                total.incrementAndGet();

                String id = String.valueOf(cls.get("id"));
                String name = String.valueOf(cls.get("display_name"));
                String nsCode = cls.get("ns_code") == null ? "" : String.valueOf(cls.get("ns_code"));
                String desc = cls.get("description") == null ? "" : String.valueOf(cls.get("description"));

                // 构建源文本：名称 + 描述
                String sourceText = desc.isBlank() ? name : name + " " + desc;

                try {
                    // 生成向量
                    float[] embedding = embeddingService.embed(sourceText);

                    // 转为 List<Float>
                    List<Float> embeddingList = new ArrayList<>(embedding.length);
                    for (float v : embedding) {
                        embeddingList.add(v);
                    }

                    // 构建完整字段
                    String apiName = cls.get("api_name") != null ? String.valueOf(cls.get("api_name")) : "";
                    pks.add(id);
                    entityTypes.add("class");
                    entityIds.add(id);
                    parentIds.add("");
                    nsCodes.add(nsCode);
                    sourceTexts.add(sourceText);
                    apiNames.add(apiName);
                    embeddings.add(embeddingList);

                    success.incrementAndGet();

                    // 批量插入
                    if (pks.size() >= batchSize) {
                        insertBatch(collectionName, pks, entityTypes, entityIds, parentIds,
                                  nsCodes, sourceTexts, apiNames, embeddings);
                        pks.clear();
                        entityTypes.clear();
                        entityIds.clear();
                        parentIds.clear();
                        nsCodes.clear();
                        sourceTexts.clear();
                        apiNames.clear();
                        embeddings.clear();
                    }

                } catch (Exception e) {
                    log.error("生成向量失败: id={}, name={}, error={}", id, name, e.getMessage());
                    failed.incrementAndGet();
                }
            }

            // 插入剩余批次
            if (!pks.isEmpty()) {
                insertBatch(collectionName, pks, entityTypes, entityIds, parentIds,
                          nsCodes, sourceTexts, apiNames, embeddings);
            }

            // 3. 统计结果（Milvus 会自动刷新，无需显式 flush）
            long count = getCollectionCount(collectionName);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("message", "向量同步完成");
            result.put("total", total.get());
            result.put("succeeded", success.get());
            result.put("failed", failed.get());
            result.put("collectionCount", count);
            result.put("collection", collectionName);

            log.info("向量同步完成: total={}, success={}, failed={}, collectionCount={}",
                total.get(), success.get(), failed.get(), count);

            return result;

        } catch (Exception e) {
            log.error("向量同步失败", e);
            return error("同步失败: " + e.getMessage());
        }
    }

    /**
     * 重建集合：删除并重新创建 collection，然后同步数据
     */
    public Map<String, Object> rebuild() {
        MilvusServiceClient mc = milvusClient();
        if (mc == null || milvusConfig == null) {
            return error("Milvus 客户端未初始化，请检查配置");
        }

        try {
            log.info("重建 Milvus collection");
            milvusConfig.rebuildCollection(mc);
            Thread.sleep(1000); // 等待集合就绪
            return syncAll();
        } catch (Exception e) {
            log.error("重建集合失败", e);
            return error("重建失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前状态
     */
    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("success", true);

        MilvusServiceClient mc = milvusClient();
        if (mc == null || milvusConfig == null) {
            status.put("available", false);
            status.put("message", "Milvus 客户端未初始化");
            return status;
        }

        try {
            String collectionName = milvusConfig.getCollectionName();
            long count = getCollectionCount(collectionName);

            status.put("available", true);
            status.put("collection", collectionName);
            status.put("count", count);
            status.put("dimension", engineConfig.getVector().getDimension());
            status.put("embeddingProvider", embeddingService.getClass().getSimpleName());

        } catch (Exception e) {
            status.put("success", false);
            status.put("available", false);
            status.put("error", e.getMessage());
            log.error("获取 Milvus 状态失败", e);
        }

        return status;
    }

    // ==================== 私有方法 ====================

    private void insertBatch(String collectionName,
                           List<String> pks,
                           List<String> entityTypes,
                           List<String> entityIds,
                           List<String> parentIds,
                           List<String> nsCodes,
                           List<String> sourceTexts,
                           List<String> apiNames,
                           List<List<Float>> embeddings) {
        try {
            List<InsertParam.Field> fields = new ArrayList<>();
            fields.add(new InsertParam.Field("pk", pks));
            fields.add(new InsertParam.Field("entity_type", entityTypes));
            fields.add(new InsertParam.Field("entity_id", entityIds));
            fields.add(new InsertParam.Field("parent_id", parentIds));
            fields.add(new InsertParam.Field("ns_code", nsCodes));
            fields.add(new InsertParam.Field("source_text", sourceTexts));
            fields.add(new InsertParam.Field("api_name", apiNames));
            fields.add(new InsertParam.Field("embedding", embeddings));

            milvusClient().insert(
                InsertParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withFields(fields)
                    .build()
            );

            log.info("插入 {} 条向量到 {}", pks.size(), collectionName);

        } catch (Exception e) {
            log.error("批量插入失败", e);
            throw new RuntimeException("批量插入失败: " + e.getMessage());
        }
    }

    private long getCollectionCount(String collectionName) {
        try {
            io.milvus.param.R<io.milvus.grpc.GetCollectionStatisticsResponse> resp =
                milvusClient().getCollectionStatistics(
                    GetCollectionStatisticsParam.newBuilder()
                        .withCollectionName(collectionName)
                        .build()
                );

            if (resp.getStatus() == 0 && resp.getData() != null) {
                var response = resp.getData();
                for (int i = 0; i < response.getStatsCount(); i++) {
                    var kv = response.getStats(i);
                    if ("row_count".equals(kv.getKey())) {
                        return Long.parseLong(kv.getValue());
                    }
                }
            }
            return 0;
        } catch (Exception e) {
            log.warn("获取集合统计失败: {}", e.getMessage());
            return 0;
        }
    }

    private Map<String, Object> success(String msg) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", true);
        r.put("message", msg);
        return r;
    }

    private Map<String, Object> error(String msg) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", false);
        r.put("message", msg);
        return r;
    }

    /**
     * 同步同义词到向量库
     */
    public Map<String, Object> syncSynonyms() {
        MilvusServiceClient mc = milvusClient();
        if (mc == null || milvusConfig == null) {
            return error("Milvus 客户端未初始化");
        }

        try {
            String collectionName = milvusConfig.getCollectionName();

            // 1. 读取所有同义词数据（通用词典）
            String sql = """
                SELECT id, word, synonyms
                FROM sys_synonym_dict
                ORDER BY id
                """;

            List<Map<String, Object>> dictEntries = jdbcTemplate.queryForList(sql);

            if (dictEntries.isEmpty()) {
                return success("没有同义词数据需要同步");
            }

            log.info("读取 {} 条同义词词典记录", dictEntries.size());

            // 2. 批量生成向量并插入
            int batchSize = 100;
            AtomicInteger total = new AtomicInteger(0);
            AtomicInteger success = new AtomicInteger(0);
            AtomicInteger failed = new AtomicInteger(0);

            List<String> pks = new ArrayList<>(batchSize);
            List<String> entityTypes = new ArrayList<>(batchSize);
            List<String> entityIds = new ArrayList<>(batchSize);
            List<String> parentIds = new ArrayList<>(batchSize);
            List<String> nsCodes = new ArrayList<>(batchSize);
            List<String> sourceTexts = new ArrayList<>(batchSize);
            List<String> apiNames = new ArrayList<>(batchSize);
            List<List<Float>> embeddings = new ArrayList<>(batchSize);

            for (Map<String, Object> entry : dictEntries) {
                total.incrementAndGet();

                String id = String.valueOf(entry.get("id"));
                String word = String.valueOf(entry.get("word"));
                String synonymsJson = String.valueOf(entry.get("synonyms"));

                // 构建源文本：主词 + 同义词列表
                String sourceText = word;
                if (synonymsJson != null && !synonymsJson.equals("null")) {
                    // synonyms 是 JSON 数组字符串，如 ["会计","资金"]
                    // 简单处理：去掉 [ ] " , 转为空格分隔
                    String synonymsText = synonymsJson
                        .replace("[", "")
                        .replace("]", "")
                        .replace("\"", "")
                        .replace(",", " ");
                    sourceText = word + " " + synonymsText;
                }

                try {
                    // 生成向量
                    float[] embedding = embeddingService.embed(sourceText);

                    // 转为 List<Float>
                    List<Float> embeddingList = new ArrayList<>(embedding.length);
                    for (float v : embedding) {
                        embeddingList.add(v);
                    }

                    // 主键格式：synonym-{id}
                    String pk = "synonym-" + id;

                    pks.add(pk);
                    entityTypes.add("synonym");
                    entityIds.add(id);
                    parentIds.add("");
                    nsCodes.add("");
                    sourceTexts.add(sourceText);
                    apiNames.add(word);  // 使用 word 作为 api_name
                    embeddings.add(embeddingList);

                    success.incrementAndGet();

                    // 批量插入
                    if (pks.size() >= batchSize) {
                        insertBatch(collectionName, pks, entityTypes, entityIds, parentIds,
                                  nsCodes, sourceTexts, apiNames, embeddings);
                        pks.clear();
                        entityTypes.clear();
                        entityIds.clear();
                        parentIds.clear();
                        nsCodes.clear();
                        sourceTexts.clear();
                        apiNames.clear();
                        embeddings.clear();
                    }

                } catch (Exception e) {
                    log.error("生成同义词向量失败: id={}, word={}, error={}",
                            id, word, e.getMessage());
                    failed.incrementAndGet();
                }
            }

            // 插入剩余批次
            if (!pks.isEmpty()) {
                insertBatch(collectionName, pks, entityTypes, entityIds, parentIds,
                          nsCodes, sourceTexts, apiNames, embeddings);
            }

            // 3. 统计结果
            long count = getCollectionCount(collectionName);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("message", "同义词向量同步完成");
            result.put("total", total.get());
            result.put("succeeded", success.get());
            result.put("failed", failed.get());
            result.put("collectionCount", count);
            result.put("collection", collectionName);

            log.info("同义词向量同步完成: total={}, success={}, failed={}, collectionCount={}",
                total.get(), success.get(), failed.get(), count);

            return result;

        } catch (Exception e) {
            log.error("同义词向量同步失败", e);
            return error("同步失败: " + e.getMessage());
        }
    }
}
