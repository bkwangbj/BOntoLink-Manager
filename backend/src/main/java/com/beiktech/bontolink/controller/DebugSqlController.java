package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.model.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 调试用 SQL 查询接口 (仅开发环境使用)
 *
 * ⚠️ 生产环境应禁用此接口或加鉴权保护
 */
@RestController
@RequestMapping("/api/debug")
public class DebugSqlController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * POST /api/debug/query
     * Body: { "sql": "SELECT * FROM ont_enum_types LIMIT 10" }
     */
    @PostMapping("/query")
    public R<List<Map<String, Object>>> executeQuery(@RequestBody Map<String, String> req) {
        String sql = req.get("sql");
        if (sql == null || sql.trim().isEmpty()) {
            return R.error(400, "SQL 不能为空");
        }

        // 基础安全检查: 只允许 SELECT (防误删/改)
        String upperSql = sql.trim().toUpperCase();
        if (!upperSql.startsWith("SELECT")) {
            return R.error(403, "仅支持 SELECT 查询");
        }

        try {
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
            return R.ok(result);
        } catch (Exception e) {
            return R.error(500, "SQL 执行失败: " + e.getMessage());
        }
    }

    /**
     * GET /api/debug/tables
     * 列出所有表
     */
    @GetMapping("/tables")
    public R<List<Map<String, Object>>> listTables() {
        try {
            String sql = "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name";
            List<Map<String, Object>> tables = jdbcTemplate.queryForList(sql);
            return R.ok(tables);
        } catch (Exception e) {
            return R.error(500, "查询表列表失败: " + e.getMessage());
        }
    }

    /**
     * GET /api/debug/schema/{tableName}
     * 查看表结构
     */
    @GetMapping("/schema/{tableName}")
    public R<List<Map<String, Object>>> getSchema(@PathVariable String tableName) {
        try {
            String sql = "PRAGMA table_info(" + tableName + ")";
            List<Map<String, Object>> columns = jdbcTemplate.queryForList(sql);
            return R.ok(columns);
        } catch (Exception e) {
            return R.error(500, "查询表结构失败: " + e.getMessage());
        }
    }
}
