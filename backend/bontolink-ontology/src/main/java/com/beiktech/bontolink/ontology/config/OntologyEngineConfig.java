package com.beiktech.bontolink.ontology.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 本体引擎配置
 */
@Data
@Configuration(proxyBeanMethods = false)
@ConfigurationProperties(prefix = "bontolink.ontology")
public class OntologyEngineConfig {

    /**
     * 是否启用 Jena OntModel（默认启用）
     * false = 完全跳过 Jena，只做同义词扩展和关键词匹配
     */
    private boolean jenaEnabled = true;

    /**
     * 是否启用推理（默认启用）
     */
    private boolean reasoningEnabled = true;

    /**
     * 推理器类型：RDFS | OWL | OWL_MICRO | NONE
     */
    private String reasonerType = "OWL";

    /**
     * 本体缓存模式：MEMORY | TDB2 | NONE
     */
    private String storageMode = "MEMORY";

    /**
     * TDB2 存储路径（当 storageMode=TDB2 时生效）
     */
    private String tdbPath = "./data/tdb";

    /**
     * 是否启用 SPARQL 端点
     */
    private boolean sparqlEnabled = true;

    /**
     * 默认数据获取策略
     */
    private String defaultFetchStrategy = "DIRECT_SQL";

    /**
     * SQL 生成器配置
     */
    private SqlGeneratorConfig sqlGenerator = new SqlGeneratorConfig();

    /**
     * API 配置
     */
    private ApiConfig api = new ApiConfig();

    @Data
    public static class SqlGeneratorConfig {
        /**
         * 是否生成 JOIN（false 则生成多个独立查询）
         */
        private boolean enableJoin = true;

        /**
         * 最大 JOIN 深度
         */
        private int maxJoinDepth = 3;

        /**
         * 是否生成分页 SQL
         */
        private boolean enablePagination = true;

        /**
         * 默认每页大小
         */
        private int defaultPageSize = 100;
    }

    @Data
    public static class ApiConfig {
        /**
         * API 超时时间（毫秒）
         */
        private int timeoutMs = 30000;

        /**
         * 重试次数
         */
        private int retryCount = 3;

        /**
         * 是否启用缓存
         */
        private boolean cacheEnabled = true;

        /**
         * 缓存 TTL（秒）
         */
        private int cacheTtlSeconds = 300;
    }

    // ========== Fuseki 嵌入式服务器配置 ==========

    private FusekiConfig fuseki = new FusekiConfig();

    @Data
    public static class FusekiConfig {
        /** 是否启用嵌入式 Fuseki 服务器 */
        private boolean enabled = true;
        /** Fuseki 监听端口 */
        private int port = 3030;
        /** Dataset 名称，访问路径为 /ontology/sparql */
        private String datasetName = "ontology";
    }

    // ========== 向量库配置 ==========

    /**
     * 向量库配置
     */
    private VectorConfig vector = new VectorConfig();

    @Data
    public static class VectorConfig {
        /**
         * 是否启用向量库连接
         * true  = 启动时检查连接，走向量相似度检索
         * false = 跳过量库，只用关键词/本体推理
         */
        private boolean enabled = true;

        /**
         * 向量库类型：pgvector | milvus | none
         * pgvector — 嵌入 PostgreSQL 的向量扩展
         * milvus   — 独立 Milvus 服务（需先启动）
         */
        private String type = "pgvector";

        /**
         * 向量维度（需与 embedding 模型匹配）
         */
        private int dimension = 768;

        /**
         * 相似度阈值（低于此值不返回）
         */
        private double similarityThreshold = 0.6;

        /**
         * 默认 TopK
         */
        private int defaultTopK = 10;

        // ---------- 连接配置（按 type 选填） ----------

        /**
         * pgvector: 数据源 bean 名称（默认走主数据源）
         */
        private String dataSource = "default";

        /**
         * Milvus 主机地址
         */
        private String milvusHost = "127.0.0.1";

        /**
         * Milvus 端口
         */
        private int milvusPort = 19530;

        /**
         * Milvus 本地安装路径（启动检查用）
         * 例如 Windows: D:\\soft\\milvus
         * 例如 Linux:   /opt/milvus
         */
        private String milvusHome = "";

        /**
         * Milvus 集合名
         */
        private String milvusCollection = "bontolink_embeddings";
    }
}
