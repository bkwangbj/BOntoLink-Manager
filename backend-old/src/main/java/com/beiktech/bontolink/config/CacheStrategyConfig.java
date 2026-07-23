package com.beiktech.bontolink.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * JetCache 缓存策略配置
 *
 * 缓存类型通过 bontolink.cache.type 控制：
 * - local（默认）: 仅使用 Caffeine 本地缓存
 * - remote: 使用 Caffeine + Redis 二级缓存，需额外配置：
 *   1. 在 application.yml 中取消注释 jetcache.remote 配置块
 *   2. 配置 spring.data.redis 连接参数
 */
@Configuration
public class CacheStrategyConfig {

    /**
     * 当 type=local 时禁用 Redis 自动配置
     * JetCache 会因为找不到 RedisConnectionFactory 而跳过 remote 初始化
     */
    @Configuration
    @ConditionalOnProperty(name = "bontolink.cache.type", havingValue = "local", matchIfMissing = true)
    @org.springframework.boot.autoconfigure.EnableAutoConfiguration(
        exclude = {RedisAutoConfiguration.class}
    )
    static class LocalOnlyConfig {
    }
}
