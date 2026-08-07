package com.beiktech.bontolink.tool.db;

import com.beiktech.bontolink.base.datasource.DataSourceConnector;
import com.beiktech.bontolink.data.entity.SysDataSource;
import com.beiktech.bontolink.data.mapper.PhysicalTableMapper;
import com.beiktech.bontolink.service.DataSourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.*;

/**
 * 工具模块 - 数据库只读查询服务
 *
 * 提供安全的只读数据库访问能力：执行 SELECT、列出表、查看表结构。
 * 方言自适应（PostgreSQL / SQLite），并对表名做标识符白名单校验防止注入。
 *
 * 安全约束：
 * - query 只允许以 SELECT 开头（防误删/改）。
 * - 表名/视图名必须是合法标识符（字母/数字/下划线，且以字母或下划线开头）。
 */
@Slf4j
@Service
public class DatabaseToolService {

    /** 合法 SQL 标识符（表名/视图名）白名单 */
    private static final String IDENTIFIER_PATTERN = "^[A-Za-z_][A-Za-z0-9_]*$";

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    @Autowired private PhysicalTableMapper physicalTableMapper;
    @Autowired private DataSourceService dataSourceService;
    @Autowired private DataSourceConnector connector;

    public DatabaseToolService(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    /**
     * 模块状态：数据库类型与表数量。
     */
    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("databaseProduct", databaseProductName());
        status.put("dialect", isPostgreSql() ? "postgresql" : "other");
        try {
            List<Map<String, Object>> tables = (List<Map<String, Object>>) listTables().getOrDefault("tables", Collections.emptyList());
            status.put("tableCount", tables.size());
        } catch (Exception e) {
            status.put("tableCount", -1);
            status.put("error", e.getMessage());
        }
        return status;
    }

    /**
     * 执行只读 SQL（仅允许 SELECT）。
     */
    public Map<String, Object> query(String sql) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (sql == null || sql.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "SQL 不能为空");
            return result;
        }
        String upper = sql.trim().toUpperCase();
        if (!upper.startsWith("SELECT")) {
            result.put("success", false);
            result.put("message", "仅支持 SELECT 只读查询");
            return result;
        }
        try {
            long t0 = System.currentTimeMillis();
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            result.put("success", true);
            result.put("rowCount", rows.size());
            result.put("rows", rows);
            result.put("elapsedMs", System.currentTimeMillis() - t0);
            return result;
        } catch (Exception e) {
            log.warn("SQL 执行失败: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "SQL 执行失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 列出所有表 / 视图（方言自适应）。
     */
    public Map<String, Object> listTables() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> tables;
            if (isPostgreSql()) {
                tables = jdbcTemplate.queryForList(
                        "SELECT table_name AS name, table_type AS type "
                                + "FROM information_schema.tables "
                                + "WHERE table_schema = 'public' ORDER BY table_name");
            } else {
                tables = jdbcTemplate.queryForList(
                        "SELECT name, type FROM sqlite_master WHERE type IN ('table','view') ORDER BY name");
            }
            // 合并已注册的外部物理表(ont_physical_table), 让工具能列出业务库表
            try {
                List<Map<String, Object>> phys = physicalTableMapper.listAll();
                Set<String> names = new HashSet<>();
                for (Map<String, Object> t : tables) {
                    Object n = t.get("name") != null ? t.get("name") : t.get("physical_table");
                    if (n != null) names.add(String.valueOf(n).toLowerCase());
                }
                for (Map<String, Object> p : phys) {
                    Object pn = p.get("physical_table");
                    if (pn == null) continue;
                    String nm = String.valueOf(pn);
                    if (names.add(nm.toLowerCase())) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("name", nm);
                        row.put("type", p.get("table_type") == null ? "table" : p.get("table_type"));
                        row.put("source", "external");
                        tables.add(row);
                    }
                }
            } catch (Exception ignore) {
                log.debug("合并外部物理表到表清单失败: {}", ignore.getMessage());
            }
            result.put("success", true);
            result.put("tables", tables);
            result.put("count", tables.size());
            return result;
        } catch (Exception e) {
            log.warn("列出表失败", e);
            result.put("success", false);
            result.put("message", "列出表失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 查看表结构（方言自适应）。
     */
    public Map<String, Object> schema(String table) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (table == null || !table.matches(IDENTIFIER_PATTERN)) {
            result.put("success", false);
            result.put("message", "非法的表名（仅允许字母/数字/下划线，且以字母或下划线开头）");
            return result;
        }
        try {
            // 优先走外部物理表通道: ont_physical_table 中注册的表按其 ds_id 连外部库读真实结构
            java.util.Map<String, Object> ext;
            try { ext = physicalTableMapper.findByTableName(table); } catch (Exception ignore) { ext = null; }
            if (ext != null && ext.get("ds_id") != null) {
                SysDataSource ds = dataSourceService.get(String.valueOf(ext.get("ds_id")));
                if (ds != null) {
                    return readExternalSchema(ds, table);
                }
            }

            // 退回主数据源查
            List<Map<String, Object>> columns;
            if (isPostgreSql()) {
                columns = jdbcTemplate.queryForList(
                        "SELECT column_name AS name, data_type AS type, is_nullable AS nullable, column_default AS default_value "
                                + "FROM information_schema.columns WHERE table_name = ? ORDER BY ordinal_position", table);
            } else {
                columns = jdbcTemplate.queryForList("PRAGMA table_info(" + table + ")");
            }
            result.put("success", true);
            result.put("table", table);
            result.put("columns", columns);
            result.put("count", columns.size());
            result.put("source", "meta");
            return result;
        } catch (Exception e) {
            log.warn("查看表结构失败: {}", table, e);
            result.put("success", false);
            result.put("message", "查看表结构失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 按外部数据源(sys_data_source)动态建连, 读取指定物理表(可能跨库/schema)的真实结构。
     * 结果结构保持与主库查询一致: {success, table, columns, count}。
     */
    private Map<String, Object> readExternalSchema(SysDataSource ds, String table) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> columns = new ArrayList<>();
            boolean found = false;
            try (Connection conn = connector.open(ds)) {
                DatabaseMetaData meta = conn.getMetaData();
                String catalog = conn.getCatalog();
                // catalog + 任意 schema 下查找精确表名(大小写不敏感)
                for (String schema : new String[]{null, conn.getSchema()}) {
                    try (ResultSet rs = meta.getTables(catalog, schema, table, new String[]{"TABLE", "VIEW"})) {
                        while (rs.next()) {
                            found = true;
                            String realSchema = rs.getString("TABLE_SCHEM");
                            String tableName = rs.getString("TABLE_NAME");
                            Set<String> pkCols = new HashSet<>();
                            try (ResultSet pk = meta.getPrimaryKeys(catalog, realSchema, tableName)) {
                                while (pk.next()) pkCols.add(pk.getString("COLUMN_NAME"));
                            } catch (Exception ignore) {}
                            try (ResultSet cr = meta.getColumns(catalog, realSchema, tableName, "%")) {
                                while (cr.next()) {
                                    Map<String, Object> c = new LinkedHashMap<>();
                                    String colName = cr.getString("COLUMN_NAME");
                                    c.put("name", colName);
                                    c.put("type", mapExtType(cr.getString("TYPE_NAME")));
                                    c.put("nullable", cr.getInt("NULLABLE") == DatabaseMetaData.columnNullable ? "YES" : "NO");
                                    c.put("is_key", pkCols.contains(colName) ? 1 : 0);
                                    c.put("comment", cr.getString("REMARKS"));
                                    columns.add(c);
                                }
                            }
                        }
                    } catch (Exception ignore) {}
                }
            }
            result.put("success", true);
            result.put("table", table);
            result.put("columns", columns);
            result.put("count", columns.size());
            result.put("foundExternal", found);
            result.put("source", "external:" + ds.getDsCode());
            return result;
        } catch (Exception e) {
            log.warn("查看外部数据源表结构失败: {} (ds={})", table, ds.getDsCode(), e);
            result.put("success", false);
            result.put("message", "外部数据源查询失败: " + e.getMessage());
            return result;
        }
    }

    /** 将外部 JDBC 类型名映射为统一简洁类型(与 PhysicalTableService 一致) */
    private static String mapExtType(String typeName) {
        if (typeName == null) return "string";
        String t = typeName.toLowerCase();
        if (t.contains("bool") || t.contains("bit")) return "boolean";
        if (t.contains("timestamp") || t.contains("datetime")) return "dateTime";
        if (t.contains("date")) return "date";
        if (t.contains("int") || t.contains("serial")) return "integer";
        if (t.contains("real") || t.contains("floa") || t.contains("doub")
                || t.contains("deci") || t.contains("numer") || t.contains("money")) return "decimal";
        return "string";
    }

    private boolean isPostgreSql() {
        String name = databaseProductName();
        return name != null && name.toLowerCase().contains("postgre");
    }

    private String databaseProductName() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.getMetaData().getDatabaseProductName();
        } catch (Exception e) {
            return null;
        }
    }
}
