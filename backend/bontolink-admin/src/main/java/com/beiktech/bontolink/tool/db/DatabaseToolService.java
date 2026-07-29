package com.beiktech.bontolink.tool.db;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
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
            return result;
        } catch (Exception e) {
            log.warn("查看表结构失败: {}", table, e);
            result.put("success", false);
            result.put("message", "查看表结构失败: " + e.getMessage());
            return result;
        }
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
