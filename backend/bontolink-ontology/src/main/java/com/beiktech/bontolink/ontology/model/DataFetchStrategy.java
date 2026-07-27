package com.beiktech.bontolink.ontology.model;

/**
 * 数据获取策略枚举
 */
public enum DataFetchStrategy {
    /**
     * 直接 SQL 查询（生成 SQL 去业务数据库执行）
     */
    DIRECT_SQL,

    /**
     * REST API 调用（通过配置的接口 URL 获取数据）
     */
    REST_API,

    /**
     * GraphQL 查询
     */
    GRAPHQL,

    /**
     * 消息队列（异步数据拉取）
     */
    MESSAGE_QUEUE,

    /**
     * 混合模式（优先 API，降级到 SQL）
     */
    HYBRID
}
