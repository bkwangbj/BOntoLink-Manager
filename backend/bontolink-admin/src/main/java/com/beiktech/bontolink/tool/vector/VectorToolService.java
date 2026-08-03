package com.beiktech.bontolink.tool.vector;

import com.beiktech.bontolink.base.embedding.EmbeddingService;
import com.beiktech.bontolink.ontology.config.OntologyEngineConfig;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.collection.GetCollectionStatisticsParam;
import io.milvus.param.collection.HasCollectionParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.*;

/**
 * 工具模块 - 向量库查询服务
 *
 * 基于 {@link EmbeddingService} 将查询文本向量化，对 {@code class_embeddings} 表做
 * pgvector 余弦相似度检索（生产环境为 PostgreSQL + pgvector 扩展）。
 *
 * 兼容性：
 * - 仅 PostgreSQL + pgvector 支持真正的向量检索；其他数据库（如开发用 SQLite）会优雅降级，
 *   返回明确提示而不报错。
 * - {@code class_embeddings} 表由 setup-pgvector.sql 创建，并通过增量同步写入 embedding；
 *   若表不存在，返回"向量表未初始化"提示。
 */
@Slf4j
@Service
public class VectorToolService {

    /** 向量表名（与 deploy/ontology/setup-pgvector.sql 保持一致） */
    private static final String EMBEDDING_TABLE = "class_embeddings";

    private final EmbeddingService embeddingService;
    private final OntologyEngineConfig engineConfig;
    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    @Autowired(required = false)
    private MilvusServiceClient milvusClient;

    @Value("${bontolink.ontology.vector.milvus-collection:ont_entity_embeddings}")
    private String milvusCollection;

    public VectorToolService(EmbeddingService embeddingService,
                             OntologyEngineConfig engineConfig,
                             JdbcTemplate jdbcTemplate,
                             DataSource dataSource) {
        this.embeddingService = embeddingService;
        this.engineConfig = engineConfig;
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    /**
     * 模块状态：向量库是否启用、类型、维度，以及当前环境是否支持真实检索。
     */
    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        OntologyEngineConfig.VectorConfig vc = engineConfig.getVector();
        status.put("enabled", vc.isEnabled());
        status.put("type", vc.getType());
        status.put("dimension", vc.getDimension());
        status.put("similarityThreshold", vc.getSimilarityThreshold());
        status.put("defaultTopK", vc.getDefaultTopK());
        status.put("embeddingProvider", embeddingService.getClass().getSimpleName());

        if (!vc.isEnabled()) {
            status.put("available", false);
            status.put("note", "向量库未启用 (bontolink.ontology.vector.enabled=false)");
            return status;
        }

        String vectorType = vc.getType();

        // 如果是 Milvus 向量库
        if ("milvus".equalsIgnoreCase(vectorType)) {
            status.put("databaseProduct", "Milvus");
            if (milvusClient == null) {
                status.put("available", false);
                status.put("note", "Milvus 客户端未初始化（检查配置 bontolink.ontology.vector.milvus-host/port）");
                return status;
            }

            try {
                Boolean exists = milvusClient.hasCollection(
                        HasCollectionParam.newBuilder()
                                .withCollectionName(milvusCollection)
                                .build()
                ).getData();

                if (!Boolean.TRUE.equals(exists)) {
                    status.put("available", false);
                    status.put("note", "Milvus collection '" + milvusCollection + "' 不存在，请访问 /api/tool/synonym/milvus/rebuild 初始化");
                    return status;
                }

                // 获取向量数量
                var statsResp = milvusClient.getCollectionStatistics(
                        GetCollectionStatisticsParam.newBuilder()
                                .withCollectionName(milvusCollection)
                                .build()
                );
                long rowCount = 0;
                if (statsResp.getData() != null) {
                    // Milvus 2.2.x: getData().getStats(index) 遍历 KeyValuePair
                    var response = statsResp.getData();
                    for (int i = 0; i < response.getStatsCount(); i++) {
                        var kv = response.getStats(i);
                        if ("row_count".equals(kv.getKey())) {
                            try {
                                rowCount = Long.parseLong(kv.getValue());
                            } catch (Exception ignored) {}
                            break;
                        }
                    }
                }

                status.put("available", true);
                status.put("collection", milvusCollection);
                status.put("vectorCount", rowCount);
                status.put("note", "Milvus 向量库已初始化，当前向量数: " + rowCount);
                return status;

            } catch (Exception e) {
                log.error("检查 Milvus 状态失败", e);
                status.put("available", false);
                status.put("note", "Milvus 连接失败: " + e.getMessage());
                return status;
            }
        }

        // PostgreSQL + pgvector
        boolean pg = isPostgreSql();
        status.put("databaseProduct", databaseProductName());
        status.put("available", pg);
        if (!pg) {
            status.put("note", "当前数据库非 PostgreSQL，无法执行 pgvector 相似度检索（仅生产 PostgreSQL 支持）");
            return status;
        }

        // PostgreSQL 环境：检查表是否存在及数据量
        try {
            Integer exists = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM information_schema.tables WHERE table_name = ?",
                    Integer.class, EMBEDDING_TABLE);
            boolean tableExists = exists != null && exists > 0;
            status.put("tableExists", tableExists);
            if (tableExists) {
                Integer cnt = jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM " + EMBEDDING_TABLE, Integer.class);
                status.put("vectorCount", cnt);
            } else {
                status.put("note", "向量表 " + EMBEDDING_TABLE + " 不存在，请先执行 setup-pgvector.sql 并同步 embedding");
            }
        } catch (Exception e) {
            status.put("available", false);
            status.put("error", e.getMessage());
        }
        return status;
    }

    /**
     * 文本相似度检索。
     *
     * @param text      查询文本
     * @param topK      返回条数（<=0 用默认）
     * @param threshold 相似度阈值（<=0 用配置默认）
     * @param nsCode    可选 namespace 分区过滤
     * @return 检索结果（含 similarity 分数）或降级提示
     */
    public Map<String, Object> search(String text, int topK, double threshold, String nsCode) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (text == null || text.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "查询文本不能为空");
            return result;
        }
        OntologyEngineConfig.VectorConfig vc = engineConfig.getVector();
        if (!vc.isEnabled()) {
            result.put("success", false);
            result.put("message", "向量库未启用 (bontolink.ontology.vector.enabled=false)");
            return result;
        }

        int k = topK > 0 ? topK : vc.getDefaultTopK();
        double simThreshold = threshold > 0 ? threshold : vc.getSimilarityThreshold();

        String vectorType = vc.getType();

        // Milvus 向量搜索
        if ("milvus".equalsIgnoreCase(vectorType)) {
            return searchMilvus(text, k, simThreshold, nsCode);
        }

        // PostgreSQL + pgvector
        if (!isPostgreSql()) {
            result.put("success", false);
            result.put("available", false);
            result.put("message", "当前数据库非 PostgreSQL，无法执行 pgvector 相似度检索（仅生产 PostgreSQL 支持）");
            return result;
        }

        try {
            // 1. 向量化
            float[] vec = embeddingService.embed(text);
            String vecLiteral = toPgVector(vec);

            // 2. 组装相似度 SQL（余弦距离 <=> ，相似度 = 1 - 距离）
            String sql;
            Object[] params;
            if (nsCode != null && !nsCode.isBlank()) {
                sql = "SELECT class_id, ns_code, source_text, 1 - (embedding <=> ?::vector) AS similarity "
                        + "FROM " + EMBEDDING_TABLE + " WHERE ns_code = ? "
                        + "ORDER BY embedding <=> ?::vector LIMIT ?";
                params = new Object[]{vecLiteral, nsCode, vecLiteral, k};
            } else {
                sql = "SELECT class_id, ns_code, source_text, 1 - (embedding <=> ?::vector) AS similarity "
                        + "FROM " + EMBEDDING_TABLE + " ORDER BY embedding <=> ?::vector LIMIT ?";
                params = new Object[]{vecLiteral, vecLiteral, k};
            }

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params);
            // 3. 阈值过滤
            List<Map<String, Object>> filtered = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                Object simObj = row.get("similarity");
                double sim = simObj instanceof Number ? ((Number) simObj).doubleValue() : 0.0;
                if (sim >= simThreshold) filtered.add(row);
            }

            result.put("success", true);
            result.put("query", text);
            result.put("nsCode", nsCode);
            result.put("similarityThreshold", simThreshold);
            result.put("matches", filtered);
            result.put("matchCount", filtered.size());
            return result;
        } catch (Exception e) {
            String msg = e.getMessage();
            // 向量表不存在的友好提示
            if (msg != null && (msg.contains("does not exist") || msg.contains("不存在")
                    || msg.contains("relation") || msg.contains("vector"))) {
                result.put("success", false);
                result.put("message", "向量表 " + EMBEDDING_TABLE
                        + " 不存在或未启用 pgvector 扩展，请先执行 setup-pgvector.sql 并同步 embedding");
                return result;
            }
            log.warn("向量检索失败", e);
            result.put("success", false);
            result.put("message", "向量检索失败: " + e.getMessage());
            return result;
        }
    }

    private Map<String, Object> searchMilvus(String text, int topK, double threshold, String nsCode) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (milvusClient == null) {
            result.put("success", false);
            result.put("message", "Milvus 客户端未初始化");
            return result;
        }

        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                float[] vec = embeddingService.embed(text);
                List<Float> vecList = new ArrayList<>(vec.length);
                for (float v : vec) vecList.add(v);

                io.milvus.param.dml.SearchParam.Builder builder =
                        io.milvus.param.dml.SearchParam.newBuilder()
                                .withCollectionName(milvusCollection)
                                .withMetricType(io.milvus.param.MetricType.IP)
                                .withVectorFieldName("embedding")
                                .withVectors(List.of(vecList))
                                .withTopK(topK)
                                .withOutFields(List.of("entity_type", "entity_id", "parent_id", "ns_code", "source_text", "api_name"));
                if (nsCode != null && !nsCode.isBlank()) {
                    builder.withExpr("ns_code == \"" + nsCode + "\"");
                }

                var searchResp = milvusClient.search(builder.build());
                if (searchResp.getData() == null) {
                    result.put("success", false);
                    result.put("message", "Milvus 搜索失败: " + searchResp.getException());
                    return result;
                }

                var wrapper = new io.milvus.response.SearchResultsWrapper(searchResp.getData().getResults());
                List<io.milvus.response.SearchResultsWrapper.IDScore> idScores = wrapper.getIDScore(0);

                List<Map<String, Object>> matches = new ArrayList<>();
                // 方案A：同一实体有主词 + 各同义词多条向量，按 entity_id 去重取最高分，
                // 保证 topK 返回的是不同实体，而非同一实体的多个同义词命中。
                Map<String, Map<String, Object>> bestByEntity = new LinkedHashMap<>();
                for (var idScore : idScores) {
                    if (idScore.getScore() < threshold) continue;
                    Map<String, Object> match = new LinkedHashMap<>();
                    match.put("pk", idScore.getStrID());
                    match.put("similarity", idScore.getScore());
                    Map<String, Object> fields = idScore.getFieldValues();
                    if (fields != null) match.putAll(fields);
                    String eId = match.get("entity_id") instanceof String s && !s.isBlank()
                            ? s : idScore.getStrID();
                    Map<String, Object> prev = bestByEntity.get(eId);
                    if (prev == null || ((Number) prev.get("similarity")).doubleValue() < idScore.getScore()) {
                        bestByEntity.put(eId, match);
                    }
                }
                matches.addAll(bestByEntity.values());

                result.put("success", true);
                result.put("query", text);
                result.put("nsCode", nsCode);
                result.put("similarityThreshold", threshold);
                result.put("matches", matches);
                result.put("matchCount", matches.size());
                return result;

            } catch (Exception e) {
                String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                boolean connErr = msg.contains("UNAVAILABLE") || msg.contains("DEADLINE_EXCEEDED")
                        || msg.contains("channel") || msg.contains("connect") || msg.contains("reset")
                        || msg.contains("end-of-stream") || msg.contains("INTERNAL")
                        || msg.contains("has not been loaded") || msg.contains("checkIfLoaded");
                if (attempt == 1 && connErr) {
                    // 第一次失败且是连接类错误：重新 load collection 后重试一次
                    log.warn("Milvus 搜索连接异常（第{}次），重新 load collection 后重试: {}", attempt, msg);
                    try {
                        milvusClient.loadCollection(
                                io.milvus.param.collection.LoadCollectionParam.newBuilder()
                                        .withCollectionName(milvusCollection).build());
                        Thread.sleep(500);  // 等待连接稳定
                    } catch (Exception ignored) {}
                    continue;
                }
                log.error("Milvus 搜索失败（第{}次）", attempt, e);
                result.put("success", false);
                result.put("message", "Milvus 搜索失败: " + msg);
                return result;
            }
        }
        result.put("success", false);
        result.put("message", "Milvus 搜索重试后仍失败");
        return result;
    }

    /**
     * 分页浏览向量库全部内容（只读）。
     */
    public Map<String, Object> list(String nsCode, int page, int size) {
        OntologyEngineConfig.VectorConfig vc = engineConfig.getVector();
        if (!vc.isEnabled()) return err("向量库未启用 (bontolink.ontology.vector.enabled=false)");

        String vectorType = vc.getType();
        if ("milvus".equalsIgnoreCase(vectorType)) {
            return listMilvus(nsCode, page, size);
        }

        // PostgreSQL + pgvector
        Map<String, Object> notReady = checkReadyPostgres();
        if (notReady != null) return notReady;
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            int p = Math.max(1, page);
            int s = size <= 0 ? 50 : size;
            int offset = (p - 1) * s;
            boolean filter = nsCode != null && !nsCode.isBlank();
            String where = filter ? " WHERE ns_code = ?" : "";

            Integer total = filter
                    ? jdbcTemplate.queryForObject("SELECT count(*) FROM " + EMBEDDING_TABLE + where, Integer.class, nsCode)
                    : jdbcTemplate.queryForObject("SELECT count(*) FROM " + EMBEDDING_TABLE, Integer.class);

            String sql = "SELECT class_id, ns_code, source_text, version, created_at, updated_at, "
                    + "vector_dimension(embedding) AS dimension FROM " + EMBEDDING_TABLE + where
                    + " ORDER BY class_id LIMIT ? OFFSET ?";
            List<Map<String, Object>> items = filter
                    ? jdbcTemplate.queryForList(sql, nsCode, s, offset)
                    : jdbcTemplate.queryForList(sql, s, offset);

            result.put("success", true);
            result.put("nsCode", nsCode);
            result.put("items", items);
            result.put("total", total == null ? 0 : total);
            result.put("page", p);
            result.put("size", s);
            return result;
        } catch (Exception e) {
            log.warn("浏览向量库失败", e);
            result.put("success", false);
            result.put("message", "浏览向量库失败: " + e.getMessage());
            return result;
        }
    }

    private Map<String, Object> listMilvus(String nsCode, int page, int size) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (milvusClient == null) return err("Milvus 客户端未初始化");

        try {
            int p = Math.max(1, page);
            int s = size <= 0 ? 50 : size;
            long offset = (long) (p - 1) * s;

            io.milvus.param.dml.QueryParam.Builder builder =
                    io.milvus.param.dml.QueryParam.newBuilder()
                            .withCollectionName(milvusCollection)
                            .withExpr(nsCode != null && !nsCode.isBlank()
                                    ? "ns_code == \"" + nsCode + "\""
                                    : "entity_type != \"__none__\"")   // Milvus 2.2.x: 用有效字段条件
                            .withOutFields(List.of("entity_type", "entity_id", "parent_id", "ns_code", "source_text", "api_name"))
                            .withOffset(offset)
                            .withLimit((long) s);

            var queryResp = milvusClient.query(builder.build());
            String qErr = queryResp.getException() != null ? queryResp.getException().getMessage() : null;
            if (qErr != null && qErr.contains("has not been loaded")) {
                // collection 未加载：loadCollection 后重试一次
                log.warn("Milvus list: collection 未加载，重新 load 后重试");
                milvusClient.loadCollection(
                        io.milvus.param.collection.LoadCollectionParam.newBuilder()
                                .withCollectionName(milvusCollection).build());
                queryResp = milvusClient.query(builder.build());
            }
            if (queryResp.getData() == null) {
                result.put("success", false);
                result.put("message", "Milvus query 失败: " + queryResp.getException());
                return result;
            }

            var wrapper = new io.milvus.response.QueryResultsWrapper(queryResp.getData());
            List<io.milvus.response.QueryResultsWrapper.RowRecord> rows = wrapper.getRowRecords();

            List<Map<String, Object>> items = new ArrayList<>();
            for (var row : rows) {
                items.add(new LinkedHashMap<>(row.getFieldValues()));
            }

            // 获取总数
            long totalCount = 0;
            try {
                var statsResp = milvusClient.getCollectionStatistics(
                        io.milvus.param.collection.GetCollectionStatisticsParam.newBuilder()
                                .withCollectionName(milvusCollection)
                                .build());
                if (statsResp.getData() != null) {
                    for (int i = 0; i < statsResp.getData().getStatsCount(); i++) {
                        var kv = statsResp.getData().getStats(i);
                        if ("row_count".equals(kv.getKey())) {
                            totalCount = Long.parseLong(kv.getValue());
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {}

            result.put("success", true);
            result.put("nsCode", nsCode);
            result.put("items", items);
            result.put("total", totalCount);
            result.put("page", p);
            result.put("size", s);
            return result;

        } catch (Exception e) {
            log.error("Milvus list 失败", e);
            result.put("success", false);
            result.put("message", "Milvus list 失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 单条向量详情（按 pk，只读）。
     */
    public Map<String, Object> detail(String classId) {
        OntologyEngineConfig.VectorConfig vc = engineConfig.getVector();
        if (!vc.isEnabled()) return err("向量库未启用 (bontolink.ontology.vector.enabled=false)");

        if ("milvus".equalsIgnoreCase(vc.getType())) {
            return detailMilvus(classId);
        }

        Map<String, Object> notReady = checkReadyPostgres();
        if (notReady != null) return notReady;
        Map<String, Object> result = new LinkedHashMap<>();
        if (classId == null || classId.isBlank()) {
            result.put("success", false);
            result.put("message", "classId 不能为空");
            return result;
        }
        try {
            String sql = "SELECT class_id, ns_code, source_text, version, created_at, updated_at, "
                    + "vector_dimension(embedding) AS dimension FROM " + EMBEDDING_TABLE + " WHERE class_id = ?";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, classId);
            if (rows.isEmpty()) {
                result.put("success", false);
                result.put("message", "未找到向量: " + classId);
                return result;
            }
            result.put("success", true);
            result.put("item", rows.get(0));
            return result;
        } catch (Exception e) {
            log.warn("查询向量详情失败: {}", classId, e);
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
            return result;
        }
    }

    private Map<String, Object> detailMilvus(String pk) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (milvusClient == null) return err("Milvus 客户端未初始化");
        if (pk == null || pk.isBlank()) return err("pk 不能为空");
        try {
            io.milvus.param.dml.QueryParam queryParam =
                    io.milvus.param.dml.QueryParam.newBuilder()
                            .withCollectionName(milvusCollection)
                            // 方案A后 pk 为 entityType:entityId:seq，传入的可能是旧格式或 entity_id，模糊兼容
                            .withExpr("entity_id == \"" + pk + "\" || pk == \"" + pk + "\"")
                            .withOutFields(List.of("entity_type", "entity_id", "parent_id", "ns_code", "source_text", "api_name"))
                            .build();
            var queryResp = milvusClient.query(queryParam);
            String qErr = queryResp.getException() != null ? queryResp.getException().getMessage() : null;
            if (qErr != null && qErr.contains("has not been loaded")) {
                // collection 未加载：loadCollection 后重试一次
                log.warn("Milvus detail: collection 未加载，重新 load 后重试");
                milvusClient.loadCollection(
                        io.milvus.param.collection.LoadCollectionParam.newBuilder()
                                .withCollectionName(milvusCollection).build());
                queryResp = milvusClient.query(queryParam);
            }
            if (queryResp.getData() == null) return err("查询失败: " + queryResp.getException());

            var wrapper = new io.milvus.response.QueryResultsWrapper(queryResp.getData());
            List<io.milvus.response.QueryResultsWrapper.RowRecord> rows = wrapper.getRowRecords();
            if (rows.isEmpty()) return err("未找到向量: " + pk);

            result.put("success", true);
            result.put("item", new LinkedHashMap<>(rows.get(0).getFieldValues()));
            return result;
        } catch (Exception e) {
            log.warn("Milvus 详情查询失败: {}", pk, e);
            result.put("success", false);
            result.put("message", "查询失败: " + e.getMessage());
            return result;
        }
    }

    /** PostgreSQL 就绪检查（非 PG 或表不存在时返回错误 map，否则返回 null）。 */
    private Map<String, Object> checkReadyPostgres() {
        if (!isPostgreSql()) return err("当前数据库非 PostgreSQL，无法浏览向量库（仅生产 PostgreSQL 支持）");
        try {
            Integer exists = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM information_schema.tables WHERE table_name = ?",
                    Integer.class, EMBEDDING_TABLE);
            if (exists == null || exists == 0) {
                return err("向量表 " + EMBEDDING_TABLE + " 不存在，请先执行 setup-pgvector.sql 并同步 embedding");
            }
        } catch (Exception e) {
            return err("检查向量表失败: " + e.getMessage());
        }
        return null;
    }

    private Map<String, Object> err(String msg) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", false);
        r.put("message", msg);
        return r;
    }

    /** 将 float[] 转为 pgvector 字面量 "[0.123,0.456,...]" */
    private String toPgVector(float[] vec) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vec.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(vec[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    private boolean isPostgreSql() {
        String name = databaseProductName();
        return name != null && name.toLowerCase().contains("postgre");
    }

    private String databaseProductName() {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            return meta.getDatabaseProductName();
        } catch (Exception e) {
            return null;
        }
    }
}
