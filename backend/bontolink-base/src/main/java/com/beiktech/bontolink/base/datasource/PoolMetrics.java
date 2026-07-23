package com.beiktech.bontolink.base.datasource;

/**
 * 连接池运行时指标，从 HikariPoolMXBean 采集。
 * <p>可用方法视 HikariCP 版本而定（5.0.1: active/idle/total/threadsAwaiting）。</p>
 */
public class PoolMetrics {

    /** 当前活跃连接数 */
    private int activeConnections;

    /** 当前空闲连接数 */
    private int idleConnections;

    /** 池中总连接数（活跃 + 空闲） */
    private int totalConnections;

    /** 等待获取连接的线程数 */
    private int threadsAwaitingConnection;

    public int getActiveConnections() { return activeConnections; }
    public void setActiveConnections(int activeConnections) { this.activeConnections = activeConnections; }

    public int getIdleConnections() { return idleConnections; }
    public void setIdleConnections(int idleConnections) { this.idleConnections = idleConnections; }

    public int getTotalConnections() { return totalConnections; }
    public void setTotalConnections(int totalConnections) { this.totalConnections = totalConnections; }

    public int getThreadsAwaitingConnection() { return threadsAwaitingConnection; }
    public void setThreadsAwaitingConnection(int threadsAwaitingConnection) { this.threadsAwaitingConnection = threadsAwaitingConnection; }
}
