package com.beiktech.bontolink.service;

import com.beiktech.bontolink.common.DataSourceConnector;
import com.beiktech.bontolink.entity.SysDataSource;
import com.beiktech.bontolink.mapper.SysDataSourceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
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
     * 监控指标。
     * <p><b>基础信息为真实探测</b>:每次调用对数据源发起真实 JDBC 连接(最多 3 次),
     * 得到真实的连通状态、响应耗时(min/avg/max)、数据库产品名+版本、驱动信息。
     * <p><b>连接池运行时指标</b>(活跃/空闲/排队连接、执行/提交/回滚次数、QPS、24h 趋势、
     * 健康统计)需接入真实连接池采集(Druid/HikariCP MXBean 或 Agent)才能获得,
     * 当前不再伪造演示数据,统一以 {@code poolAvailable=false} 标识,由前端提示"暂未接入采集"。
     */
    public Map<String, Object> monitor(String id) {
        SysDataSource d = mapper.findById(id);
        Map<String, Object> r = new LinkedHashMap<>();
        if (d == null) return r;

        int max = d.getMaxConn() == null ? 100 : d.getMaxConn();

        // —— 真实探测:连接状态 / 响应耗时 / 数据库版本 ——
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
                try (Connection conn = connector.open(d)) {
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
        // 真实探测结果同步回库(与"测试连接"一致)
        mapper.updateMonitor(id, connectStatus, rtAvg, 0);

        Map<String, Object> basic = new LinkedHashMap<>();
        basic.put("connectStatus", connectStatus);
        basic.put("maxConn", max);
        basic.put("minIdle", 5);
        basic.put("idleTimeoutMs", 300000);
        basic.put("connectTimeoutMs", DataSourceConnector.LOGIN_TIMEOUT * 1000);
        basic.put("responseMs", rtAvg);
        basic.put("product", productName != null ? productName
                : (d.getDsType() == null ? "—" : d.getDsType().toUpperCase()));
        basic.put("version", productVersion != null ? productVersion : "—");
        basic.put("driver", driverInfo);
        basic.put("host", extractHost(d));
        basic.put("loadPct", null);          // 需连接池采集
        basic.put("poolAvailable", false);
        basic.put("probeError", probeError);

        Map<String, Object> rtBlock = new LinkedHashMap<>();
        rtBlock.put("available", !pings.isEmpty());
        rtBlock.put("min", rtMin);
        rtBlock.put("avg", rtAvg);
        rtBlock.put("max", rtMax);

        r.put("basic", basic);
        r.put("rt", rtBlock);
        r.put("poolAvailable", false);
        // 连接池运行时指标 / 健康统计 / 历史趋势:暂未接入真实采集
        r.put("detail", null);
        r.put("health", null);
        r.put("trend", new ArrayList<>());
        return r;
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
