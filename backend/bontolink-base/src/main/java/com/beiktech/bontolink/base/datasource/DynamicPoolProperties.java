package com.beiktech.bontolink.base.datasource;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 统一连接池配置属性。
 * <p>对应 application.yml 中 {@code bontolink.datasource.pool.*} 配置项，
 * 作为动态数据源连接池的全局默认值。单个数据源的 {@code max_conn} 字段可覆盖 {@link #maxPoolSize}。</p>
 */
@Component
@ConfigurationProperties(prefix = "bontolink.datasource.pool")
public class DynamicPoolProperties {

    /** 最大连接数（默认 10），可由数据源记录的 max_conn 字段覆盖 */
    private int maxPoolSize = 10;

    /** 最小空闲连接数 */
    private int minIdle = 2;

    /** 连接超时(毫秒)：等待池中可用连接的最大时间 */
    private long connectionTimeoutMs = 30000;

    /** 空闲超时(毫秒)：连接在池中保持空闲的最长时间 */
    private long idleTimeoutMs = 300000;

    /** 最大生存时间(毫秒)：连接在池中的最大寿命 */
    private long maxLifetimeMs = 1800000;

    /** 泄漏检测阈值(毫秒)，0 表示禁用 */
    private long leakDetectionThresholdMs = 0;

    public int getMaxPoolSize() { return maxPoolSize; }
    public void setMaxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; }

    public int getMinIdle() { return minIdle; }
    public void setMinIdle(int minIdle) { this.minIdle = minIdle; }

    public long getConnectionTimeoutMs() { return connectionTimeoutMs; }
    public void setConnectionTimeoutMs(long connectionTimeoutMs) { this.connectionTimeoutMs = connectionTimeoutMs; }

    public long getIdleTimeoutMs() { return idleTimeoutMs; }
    public void setIdleTimeoutMs(long idleTimeoutMs) { this.idleTimeoutMs = idleTimeoutMs; }

    public long getMaxLifetimeMs() { return maxLifetimeMs; }
    public void setMaxLifetimeMs(long maxLifetimeMs) { this.maxLifetimeMs = maxLifetimeMs; }

    public long getLeakDetectionThresholdMs() { return leakDetectionThresholdMs; }
    public void setLeakDetectionThresholdMs(long leakDetectionThresholdMs) { this.leakDetectionThresholdMs = leakDetectionThresholdMs; }
}
