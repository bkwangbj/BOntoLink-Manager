package com.beiktech.bontolink.service;

import com.beiktech.bontolink.common.DataSourceConnector;
import com.beiktech.bontolink.common.PoolMetrics;
import com.beiktech.bontolink.config.DynamicDataSourceRegistry;
import com.beiktech.bontolink.config.DynamicPoolProperties;
import com.beiktech.bontolink.data.entity.SysDataSource;
import com.beiktech.bontolink.data.mapper.SysDataSourceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.*;

/**
 * 数据源（sys_data_source）的 CRUD、连通性测试与监控指标服务。
 * <p>连接池由 {@link DynamicDataSourceRegistry} 管理，URL/用户名/密码取自 sys_data_source 表，
 * 池参数取自 {@link DynamicPoolProperties} 统一配置。探测和测试使用直连(不占用池连接)。</p>
 */
@Service
public class DataSourceService {

    @Autowired private SysDataSourceMapper mapper;
    @Autowired private com.beiktech.bontolink.data.mapper.PhysicalTableMapper physicalTableMapper;
    @Autowired private DataSourceConnector connector;
    @Autowired private DynamicDataSourceRegistry registry;
    @Autowired private DynamicPoolProperties poolProperties;

    /** 查询所有数据源。 */
    public List<SysDataSource> list() { return mapper.listAll(); }

    /** 按 id 查单条数据源。 */
    public SysDataSource get(String id) { return mapper.findById(id); }

    /**
     * 新建或更新数据源。id 为空时视为新建：自动生成 "datasource-{UUID}"，
     * 并初始化默认状态（启用、online、maxConn=100 等）；id 已存在则直接更新并刷新连接池。
     * 返回落库后的最新对象。
     */
    public SysDataSource save(SysDataSource d) {
        boolean isNew = (d.getId() == null || d.getId().isEmpty());
        if (isNew) {
            d.setId("datasource-" + UUID.randomUUID());
            if (d.getStatus() == null) d.setStatus(1);
            if (d.getRefCount() == null) d.setRefCount(0);
            if (d.getConnectStatus() == null) d.setConnectStatus("online");
            if (d.getActiveConn() == null) d.setActiveConn(0);
            if (d.getMaxConn() == null) d.setMaxConn(100);
            if (d.getResponseMs() == null) d.setResponseMs(0);
            if (d.getCollectionCnt() == null) d.setCollectionCnt(0);
            mapper.insert(d);
        } else {
            mapper.update(d);
            // 更新后刷新连接池（配置可能已变）
            try { registry.refresh(d.getId()); } catch (Exception ignored) { /* 无池则忽略 */ }
        }
        return mapper.findById(d.getId());
    }

    /**
     * 删除数据源，同时销毁其连接池并级联删除名下所有已同步的物理表记录。
     */
    public void delete(String id) {
        registry.remove(id);                // 先销毁连接池
        physicalTableMapper.deleteByDs(id); // 级联清理该数据源的物理表
        mapper.delete(id);
    }

    /**
     * 切换数据源启用/禁用状态（1→0 或 0→1），同步管理连接池生命周期。
     */
    public void toggleStatus(String id) {
        SysDataSource d = mapper.findById(id);
        if (d == null) throw new IllegalArgumentException("数据源不存在");
        int newStatus = d.getStatus() == 1 ? 0 : 1;
        mapper.updateStatus(id, newStatus);
        if (newStatus == 1) {
            try { registry.getOrCreate(id); } catch (Exception ignored) { /* 创建失败不打断流程 */ }
        } else {
            registry.remove(id);
        }
    }

    /**
     * 测试数据源连通性（使用 DriverManager 直连，不占用池连接）。
     */
    public Map<String, Object> testConnection(String id) {
        SysDataSource d = mapper.findById(id);
        Map<String, Object> r = new LinkedHashMap<>();
        if (d == null) { r.put("ok", false); r.put("msg", "数据源不存在"); return r; }

        boolean ok = false;
        String msg;
        int rt = 0;

        if (d.getStatus() != null && d.getStatus() == 0) {
            msg = "测试失败：数据源已禁用";
        } else {
            long start = System.currentTimeMillis();
            try (Connection conn = connector.openDirect(d)) {
                ok = conn.isValid(5);
                rt = (int) (System.currentTimeMillis() - start);
                msg = ok ? "测试成功（" + d.getDsType().toUpperCase() + " 连接正常）" : "测试失败：连接无效";
            } catch (Exception e) {
                rt = (int) (System.currentTimeMillis() - start);
                msg = "测试失败：" + e.getMessage();
            }
        }

        r.put("ok", ok);
        r.put("responseMs", rt);
        r.put("msg", msg);
        mapper.updateMonitor(id, ok ? "online" : "offline", rt, d.getActiveConn() == null ? 0 : d.getActiveConn());
        return r;
    }

    /** 总览统计（顶部数据源总数 / 正常 / 风险） */
    public Map<String, Object> overview() {
        List<SysDataSource> all = mapper.listAll();
        long total = all.size();
        long normal = all.stream().filter(d -> d.getStatus() != null && d.getStatus() == 1 && "online".equals(d.getConnectStatus())).count();
        long risk = all.stream().filter(d -> d.getStatus() == null || d.getStatus() == 0 || !"online".equals(d.getConnectStatus())).count();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", total);
        r.put("normal", normal);
        r.put("risk", risk);
        r.put("byType", mapper.groupByType());
        return r;
    }

    /**
     * 监控指标。
     * <p><b>基础信息为真实探测</b>: 使用 DriverManager 直连（不占用池连接），
     * 每次调用对数据源发起真实 JDBC 连接(最多 3 次)，
     * 得到真实的连通状态、响应耗时(min/avg/max)、数据库产品名+版本、驱动信息。
     * <p><b>连接池运行时指标</b>: 通过 {@link DynamicDataSourceRegistry#getMetrics} 获取 HikariCP MXBean 实时数据，
     * 包含活跃/空闲/排队连接数等。
     */
    public Map<String, Object> monitor(String id) {
        SysDataSource d = mapper.findById(id);
        Map<String, Object> r = new LinkedHashMap<>();
        if (d == null) return r;

        int max = (d.getMaxConn() != null && d.getMaxConn() > 0) ? d.getMaxConn() : poolProperties.getMaxPoolSize();

        // —— 真实探测:连接状态 / 响应耗时 / 数据库版本（用直连，不占池）——
        boolean online = false;
        String productName = null, productVersion = null, driverInfo = null, probeError = null;
        List<Integer> pings = new ArrayList<>();

        if (d.getStatus() != null && d.getStatus() == 0) {
            probeError = "数据源已禁用";
        } else if ("mongodb".equalsIgnoreCase(d.getDsType())) {
            probeError = "MongoDB 暂不支持 JDBC 探测";
        } else {
            for (int i = 0; i < 3; i++) {
                long start = System.currentTimeMillis();
                try (Connection conn = connector.openDirect(d)) {
                    if (conn.isValid(5)) {
                        online = true;
                        pings.add((int) (System.currentTimeMillis() - start));
                        if (productName == null) {
                            try {
                                DatabaseMetaData md = conn.getMetaData();
                                productName = md.getDatabaseProductName();
                                productVersion = md.getDatabaseProductVersion();
                                driverInfo = md.getDriverName() + " " + md.getDriverVersion();
                            } catch (Exception ignore) { /* 元数据不可读时忽略 */ }
                        }
                    }
                } catch (Exception e) {
                    probeError = e.getMessage();
                    break;
                }
            }
        }

        int rtMin = 0, rtAvg = 0, rtMax = 0;
        if (!pings.isEmpty()) {
            rtMin = pings.stream().mapToInt(Integer::intValue).min().orElse(0);
            rtMax = pings.stream().mapToInt(Integer::intValue).max().orElse(0);
            rtAvg = (int) Math.round(pings.stream().mapToInt(Integer::intValue).average().orElse(0));
        }
        String connectStatus = online ? "online" : "offline";

        // —— 从注册表获取连接池实时指标 ——
        PoolMetrics poolMetrics = registry.getMetrics(id);
        int activeConn = 0;
        if (poolMetrics != null) {
            activeConn = poolMetrics.getActiveConnections();
        }
        // 真实探测结果 + 池活跃连接数同步回库
        mapper.updateMonitor(id, connectStatus, rtAvg, activeConn);

        Map<String, Object> basic = new LinkedHashMap<>();
        basic.put("connectStatus", connectStatus);
        basic.put("maxConn", max);
        basic.put("minIdle", poolProperties.getMinIdle());
        basic.put("idleTimeoutMs", poolProperties.getIdleTimeoutMs());
        basic.put("connectTimeoutMs", poolProperties.getConnectionTimeoutMs());
        basic.put("responseMs", rtAvg);
        basic.put("product", productName != null ? productName
                : (d.getDsType() == null ? "—" : d.getDsType().toUpperCase()));
        basic.put("version", productVersion != null ? productVersion : "—");
        basic.put("driver", driverInfo);
        basic.put("host", extractHost(d));
        basic.put("probeError", probeError);

        Map<String, Object> rtBlock = new LinkedHashMap<>();
        rtBlock.put("available", !pings.isEmpty());
        rtBlock.put("min", rtMin);
        rtBlock.put("avg", rtAvg);
        rtBlock.put("max", rtMax);

        r.put("basic", basic);
        r.put("rt", rtBlock);

        if (poolMetrics != null) {
            // 有真实连接池指标
            basic.put("poolAvailable", true);
            basic.put("loadPct", poolMetrics.getTotalConnections() > 0
                    ? (poolMetrics.getActiveConnections() * 100 / poolMetrics.getTotalConnections()) : 0);

            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("logicConnect", poolMetrics.getActiveConnections());
            detail.put("idleConn", poolMetrics.getIdleConnections());
            detail.put("waitingConn", poolMetrics.getThreadsAwaitingConnection());
            detail.put("totalCreated", poolMetrics.getTotalConnections());
            r.put("detail", detail);
            r.put("poolAvailable", true);

            // HikariCP MXBean 不提供计数器类的健康统计，暂留 null
            r.put("health", null);
        } else {
            basic.put("poolAvailable", false);
            basic.put("loadPct", null);
            r.put("detail", null);
            r.put("health", null);
            r.put("poolAvailable", false);
        }

        r.put("trend", new ArrayList<>());
        return r;
    }

    // ========== 连接池管理方法 ==========

    /**
     * 获取指定数据源的连接池运行时指标。
     */
    public Map<String, Object> getPoolMetrics(String id) {
        SysDataSource d = mapper.findById(id);
        if (d == null) return Map.of();
        PoolMetrics pm = registry.getMetrics(id);
        if (pm == null) {
            return Map.of("poolAvailable", false, "msg", "无连接池（可能未启用或非 JDBC 类型）");
        }
        int max = (d.getMaxConn() != null && d.getMaxConn() > 0) ? d.getMaxConn() : poolProperties.getMaxPoolSize();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("poolAvailable", true);
        m.put("activeConnections", pm.getActiveConnections());
        m.put("idleConnections", pm.getIdleConnections());
        m.put("totalConnections", pm.getTotalConnections());
        m.put("threadsAwaitingConnection", pm.getThreadsAwaitingConnection());
        m.put("maxPoolSize", max);
        return m;
    }

    /**
     * 热刷新连接池（销毁 + 用当前配置重建）。
     */
    public void refreshPool(String id) {
        SysDataSource d = mapper.findById(id);
        if (d == null) throw new IllegalArgumentException("数据源不存在");
        registry.refresh(id);
    }

    /**
     * 动态调整连接池最大大小（更新 DB 记录 + 热刷新池）。
     */
    public void resizePool(String id, int newMax) {
        SysDataSource d = mapper.findById(id);
        if (d == null) throw new IllegalArgumentException("数据源不存在");
        mapper.updateMaxConn(id, newMax);
        registry.refresh(id);
    }

    /** 从 JDBC URL 或 MongoDB URL 中解析 host(:port) 部分，用于监控面板展示。 */
    private String extractHost(SysDataSource d) {
        String url = "mongodb".equals(d.getDsType()) ? d.getMongoUrl() : d.getJdbcUrl();
        if (url == null || url.isEmpty()) return "—";
        int slash = url.indexOf("//");
        if (slash < 0) return url;
        String tail = url.substring(slash + 2);
        int q = tail.indexOf('/');
        return q < 0 ? tail : tail.substring(0, q);
    }

    /** 数据源类型 → 适配的 JDBC 驱动 */
    public Map<String, List<String>> driverCatalog() {
        Map<String, List<String>> m = new LinkedHashMap<>();
        m.put("mysql",      List.of("com.mysql.cj.jdbc.Driver", "com.mysql.jdbc.Driver"));
        m.put("postgresql", List.of("org.postgresql.Driver", "com.impossibl.postgres.jdbc.PGDriver"));
        m.put("oracle",     List.of("oracle.jdbc.OracleDriver", "oracle.jdbc.driver.OracleDriver"));
        m.put("mongodb",    List.of());
        m.put("dm",         List.of("dm.jdbc.driver.DmDriver", "dm.jdbc1.DmDriver"));
        m.put("kingbase",   List.of("com.kingbase8.Driver", "com.kingbase.Driver"));
        m.put("oscar",      List.of("com.oscar.jdbc.Driver"));
        m.put("gbase",      List.of("com.gbase.jdbc.Driver", "com.gbase8a.jdbc.Driver"));
        m.put("polardb",    List.of("com.mysql.cj.jdbc.Driver", "com.aliyun.polardb.jdbc.Driver"));
        m.put("tdsql",      List.of("com.mysql.cj.jdbc.Driver", "com.tencent.tdsql.jdbc.Driver"));
        m.put("gaussdb",    List.of("com.huawei.gaussdb.jdbc.Driver", "org.postgresql.Driver"));
        m.put("oceanbase",  List.of("com.oceanbase.jdbc.Driver", "com.mysql.cj.jdbc.Driver"));
        return m;
    }
}
