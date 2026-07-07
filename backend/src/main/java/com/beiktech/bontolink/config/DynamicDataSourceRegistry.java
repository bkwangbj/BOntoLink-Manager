package com.beiktech.bontolink.config;

import com.beiktech.bontolink.common.DataSourceConnector;
import com.beiktech.bontolink.common.PoolMetrics;
import com.beiktech.bontolink.entity.SysDataSource;
import com.beiktech.bontolink.mapper.SysDataSourceMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态数据源连接池注册表。
 * <p>管理所有外部数据源的 HikariCP 连接池：启动时预创建已启用的 JDBC 数据源池，
 * 运行时通过 {@link #getOrCreate}, {@link #refresh}, {@link #remove} 管理生命周期。
 * URL / 用户名 / 密码从 {@link SysDataSource} 取，其他池参数从 {@link DynamicPoolProperties} 取。</p>
 */
@Component
public class DynamicDataSourceRegistry {

    private static final Logger log = LoggerFactory.getLogger(DynamicDataSourceRegistry.class);

    @Autowired
    private SysDataSourceMapper sysDataSourceMapper;

    @Autowired
    private DynamicPoolProperties poolProperties;

    /** dsId → HikariDataSource */
    private final ConcurrentHashMap<String, HikariDataSource> poolMap = new ConcurrentHashMap<>();

    /**
     * 启动时初始化：扫描所有已启用且有 JDBC URL 的数据源，创建连接池。
     */
    @PostConstruct
    public void init() {
        List<SysDataSource> all = sysDataSourceMapper.listAll();
        for (SysDataSource ds : all) {
            if (ds.getStatus() != null && ds.getStatus() == 1
                    && ds.getJdbcUrl() != null && !ds.getJdbcUrl().isBlank()
                    && !"mongodb".equalsIgnoreCase(ds.getDsType())) {
                try {
                    HikariDataSource hds = createDataSource(ds);
                    poolMap.put(ds.getId(), hds);
                    log.info("为数据源 [{}] ({}) 创建连接池, maxPoolSize={}",
                            ds.getDsName(), ds.getDsCode(),
                            hds.getMaximumPoolSize());
                } catch (Exception e) {
                    log.warn("为数据源 [{}] ({}) 创建连接池失败: {}", ds.getDsName(), ds.getDsCode(), e.getMessage());
                }
            }
        }
        log.info("动态数据源注册表初始化完成, 已创建 {} 个连接池", poolMap.size());
    }

    /**
     * 获取或创建数据源的连接池。
     *
     * @param dsId 数据源 ID
     * @return HikariDataSource
     */
    public HikariDataSource getOrCreate(String dsId) {
        return poolMap.computeIfAbsent(dsId, id -> {
            SysDataSource ds = sysDataSourceMapper.findById(id);
            if (ds == null) {
                throw new IllegalArgumentException("数据源不存在: " + id);
            }
            if ("mongodb".equalsIgnoreCase(ds.getDsType())) {
                throw new IllegalArgumentException("MongoDB 暂不支持连接池: " + id);
            }
            try {
                HikariDataSource hds = createDataSource(ds);
                log.info("按需创建连接池 [{}] ({})", ds.getDsName(), ds.getDsCode());
                return hds;
            } catch (Exception e) {
                throw new RuntimeException("创建连接池失败: " + ds.getDsName(), e);
            }
        });
    }

    /**
     * 刷新（销毁 + 重建）数据源的连接池。
     *
     * @param dsId 数据源 ID
     */
    public void refresh(String dsId) {
        HikariDataSource old = poolMap.remove(dsId);
        if (old != null) {
            log.info("关闭连接池 {}", old.getPoolName());
            old.close();
        }
        // 重建：重新从 DB 读取最新配置
        SysDataSource ds = sysDataSourceMapper.findById(dsId);
        if (ds != null && ds.getStatus() != null && ds.getStatus() == 1
                && ds.getJdbcUrl() != null && !ds.getJdbcUrl().isBlank()
                && !"mongodb".equalsIgnoreCase(ds.getDsType())) {
            try {
                HikariDataSource hds = createDataSource(ds);
                poolMap.put(dsId, hds);
                log.info("重建连接池 [{}] ({})", ds.getDsName(), ds.getDsCode());
            } catch (Exception e) {
                log.warn("重建连接池 [{}] 失败: {}", ds.getDsName(), e.getMessage());
            }
        }
    }

    /**
     * 移除（关闭 + 删除）数据源的连接池。
     *
     * @param dsId 数据源 ID
     */
    public void remove(String dsId) {
        HikariDataSource hds = poolMap.remove(dsId);
        if (hds != null) {
            log.info("关闭连接池 {}", hds.getPoolName());
            hds.close();
        }
    }

    /**
     * 获取连接池运行时指标。
     *
     * @param dsId 数据源 ID
     * @return PoolMetrics，如果数据源无池则返回 null
     */
    public PoolMetrics getMetrics(String dsId) {
        HikariDataSource hds = poolMap.get(dsId);
        if (hds == null) return null;
        try {
            HikariPoolMXBean mx = hds.getHikariPoolMXBean();
            PoolMetrics m = new PoolMetrics();
            m.setActiveConnections(mx.getActiveConnections());
            m.setIdleConnections(mx.getIdleConnections());
            m.setTotalConnections(mx.getTotalConnections());
            m.setThreadsAwaitingConnection(mx.getThreadsAwaitingConnection());
            return m;
        } catch (Exception e) {
            log.warn("获取连接池指标失败 [{}]: {}", dsId, e.getMessage());
            return null;
        }
    }

    /**
     * 获取所有池的状态快照（用于管理端）。
     */
    public Map<String, Integer> poolSizes() {
        Map<String, Integer> sizes = new java.util.LinkedHashMap<>();
        poolMap.forEach((id, hds) -> sizes.put(id, hds.getHikariPoolMXBean().getTotalConnections()));
        return sizes;
    }

    /**
     * 根据 SysDataSource 记录创建一个 HikariCP 连接池。
     */
    private HikariDataSource createDataSource(SysDataSource ds) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(ds.getJdbcUrl());
        config.setUsername(ds.getUsername());
        config.setPassword(ds.getPassword());
        if (ds.getJdbcDriver() != null && !ds.getJdbcDriver().isBlank()) {
            config.setDriverClassName(ds.getJdbcDriver());
        }
        // 开启注释上报,让物理表同步经连接池也能读到表/字段注释(REMARKS),中文名优先取注释
        DataSourceConnector.remarksProps(ds.getJdbcUrl()).forEach(config::addDataSourceProperty);
        config.setPoolName("pool-" + ds.getDsCode());

        // maxPoolSize: 数据源记录的 max_conn 可覆盖统一配置
        int maxConn = (ds.getMaxConn() != null && ds.getMaxConn() > 0)
                ? ds.getMaxConn() : poolProperties.getMaxPoolSize();
        config.setMaximumPoolSize(maxConn);
        config.setMinimumIdle(Math.min(poolProperties.getMinIdle(), maxConn));
        config.setConnectionTimeout(poolProperties.getConnectionTimeoutMs());
        config.setIdleTimeout(poolProperties.getIdleTimeoutMs());
        config.setMaxLifetime(poolProperties.getMaxLifetimeMs());
        if (poolProperties.getLeakDetectionThresholdMs() > 0) {
            config.setLeakDetectionThreshold(poolProperties.getLeakDetectionThresholdMs());
        }

        // 连接验证
        config.setConnectionTestQuery("SELECT 1");

        return new HikariDataSource(config);
    }

    /**
     * 应用关闭时销毁所有连接池。
     */
    @PreDestroy
    public void destroy() {
        poolMap.forEach((id, hds) -> {
            try {
                log.info("关闭连接池 {}", hds.getPoolName());
                hds.close();
            } catch (Exception e) {
                log.warn("关闭连接池异常 [{}]: {}", id, e.getMessage());
            }
        });
        poolMap.clear();
        log.info("所有动态连接池已销毁");
    }
}
