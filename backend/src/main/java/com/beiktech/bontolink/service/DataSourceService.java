package com.beiktech.bontolink.service;

import com.beiktech.bontolink.common.DataSourceConnector;
import com.beiktech.bontolink.entity.SysDataSource;
import com.beiktech.bontolink.mapper.SysDataSourceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.util.*;

@Service
public class DataSourceService {

    @Autowired private SysDataSourceMapper mapper;
    @Autowired private com.beiktech.bontolink.mapper.PhysicalTableMapper physicalTableMapper;
    @Autowired private DataSourceConnector connector;

    public List<SysDataSource> list() { return mapper.listAll(); }
    public SysDataSource get(String id) { return mapper.findById(id); }

    public SysDataSource save(SysDataSource d) {
        if (d.getId() == null || d.getId().isEmpty()) {
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
        }
        return mapper.findById(d.getId());
    }

    public void delete(String id) {
        physicalTableMapper.deleteByDs(id);   // 级联清理该数据源的物理表
        mapper.delete(id);
    }

    public void toggleStatus(String id) {
        SysDataSource d = mapper.findById(id);
        if (d == null) throw new IllegalArgumentException("数据源不存在");
        mapper.updateStatus(id, d.getStatus() == 1 ? 0 : 1);
    }

    /**
     * 测试数据源连通性（真实 JDBC 连接：按配置建连 + conn.isValid 校验，记录真实耗时）。
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
            try (Connection conn = connector.open(d)) {
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
        // 把测试结果同步到监控字段
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
     * 监控指标（演示数据：实际由 Druid/Agent 推送）
     * 返回基础监控 / 详细监控 / 历史趋势 三段
     */
    public Map<String, Object> monitor(String id) {
        SysDataSource d = mapper.findById(id);
        Map<String, Object> r = new LinkedHashMap<>();
        if (d == null) return r;
        Random rnd = new Random(id.hashCode());

        int active = d.getActiveConn() == null ? 0 : d.getActiveConn();
        int max    = d.getMaxConn()    == null ? 100 : d.getMaxConn();
        int rt     = d.getResponseMs() == null ? 0 : d.getResponseMs();

        Map<String, Object> basic = new LinkedHashMap<>();
        basic.put("connectStatus", d.getConnectStatus());
        basic.put("activeConn", active);
        basic.put("maxConn", max);
        basic.put("idleConn", Math.max(0, max / 10 - active / 2));
        basic.put("loadPct", max > 0 ? (int) Math.round(active * 100.0 / max) : 0);
        basic.put("minIdle", 5);
        basic.put("idleTimeoutMs",    300000);
        basic.put("connectTimeoutMs", 60000);
        basic.put("responseMs", rt);
        basic.put("uptimeHours", 12 + rnd.nextInt(720));
        basic.put("version", inferVersion(d.getDsType()));
        basic.put("host", extractHost(d));

        int totalCreated = 15 + active + rnd.nextInt(40);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("logicConnect",   active);
        detail.put("physicalConnect",Math.max(active + 1, 2));
        detail.put("waitThreadCount",rnd.nextInt(5));
        detail.put("notEmptyWaitMs", rnd.nextInt(50));
        detail.put("executeCount",   1000 + rnd.nextInt(20000));
        detail.put("errorCount",     rnd.nextInt(10));
        detail.put("commitCount",    500 + rnd.nextInt(10000));
        detail.put("rollbackCount",  rnd.nextInt(20));
        detail.put("recycleErrorCount", rnd.nextInt(3));
        detail.put("idleConn",       Math.max(0, max - active));
        detail.put("waitingConn",    rnd.nextInt(3));
        detail.put("totalCreated",   totalCreated);

        Map<String, Object> rtBlock = new LinkedHashMap<>();
        rtBlock.put("min", Math.max(2, rt / 4));
        rtBlock.put("avg", Math.max(rt / 2, 8));
        rtBlock.put("max", Math.max(rt + 20 + rnd.nextInt(80), 30));

        Map<String, Object> health = new LinkedHashMap<>();
        int success = 800 + rnd.nextInt(3000);
        health.put("acquireSuccess", success);
        health.put("acquireFail",    rnd.nextInt(8));
        health.put("autoReconnect",  rnd.nextInt(4));
        health.put("killedInvalid",  rnd.nextInt(3));
        health.put("leak",           rnd.nextInt(2));

        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 23; i >= 0; i--) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("hour", "T-" + i + "h");
            p.put("rt", Math.max(5, rt + rnd.nextInt(60) - 30));
            p.put("qps", 30 + rnd.nextInt(200));
            trend.add(p);
        }

        r.put("basic", basic);
        r.put("detail", detail);
        r.put("rt", rtBlock);
        r.put("health", health);
        r.put("trend", trend);
        return r;
    }

    private String inferVersion(String dsType) {
        if (dsType == null) return "—";
        return switch (dsType) {
            case "mysql" -> "8.0.30";
            case "postgresql" -> "15.4";
            case "oracle" -> "19c";
            case "mongodb" -> "6.0.8";
            case "dm" -> "DM8";
            case "kingbase" -> "V8R6";
            case "oscar" -> "7.0";
            case "gbase" -> "8s 8.8";
            case "polardb" -> "MySQL 8.0";
            case "tdsql" -> "MySQL 5.7";
            case "gaussdb" -> "GaussDB 505.1";
            case "oceanbase" -> "4.2";
            default -> "—";
        };
    }

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
