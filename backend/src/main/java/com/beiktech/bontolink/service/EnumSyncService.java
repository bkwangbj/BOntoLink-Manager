package com.beiktech.bontolink.service;

import com.beiktech.bontolink.common.DataSourceConnector;
import com.beiktech.bontolink.entity.SysDataSource;
import com.beiktech.bontolink.mapper.EnumTypeMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.*;

/**
 * 枚举数据库同步。支持两种数据来源模式：
 *  - table: 选择数据源 + 物理表 + WHERE 筛选，字段下拉映射
 *  - sql:   直接写完整 SQL，字段名手动指定（别名需与 field_code/field_name 等配置一致）
 *
 * 批量提交：新增枚举项改为批量 INSERT（每批 200 条），减少 IO 轮次。
 * 锁定保护：is_sync_locked=1 的枚举项不会被同步覆盖或删除。
 * 三种同步模式：full_overwrite / level_diff / incremental。
 */
@Slf4j
@Service
public class EnumSyncService {

    private static final int BATCH_SIZE = 200;

    @Autowired private EnumTypeMapper mapper;
    @Autowired private DataSourceService dsService;
    @Autowired private DataSourceConnector connector;

    public Map<String, Object> run(String enumId, String syncType, String operUser) {
        Map<String, Object> log = new LinkedHashMap<>();
        log.put("id", "enum-sync-log-" + UUID.randomUUID());
        log.put("enum_id", enumId);
        log.put("sync_type", syncType == null || syncType.isBlank() ? "manual" : syncType);
        log.put("oper_user", operUser);
        int add = 0, upd = 0, del = 0;

        try {
            Map<String, Object> cfg = mapper.findSyncConfig(enumId);
            if (cfg == null || isBlank(cfg.get("data_source_id")))
                throw new IllegalStateException("未配置同步规则（数据源）");

            String sourceType = str(cfg.getOrDefault("sync_source_type", "table"));
            boolean sqlMode = "sql".equals(sourceType);

            String fieldCode   = str(cfg.get("field_code"));
            String fieldName   = str(cfg.get("field_name"));
            if (fieldCode.isBlank() || fieldName.isBlank())
                throw new IllegalStateException("未配置编码字段/名称字段");

            SysDataSource ds = dsService.get(str(cfg.get("data_source_id")));
            if (ds == null) throw new IllegalStateException("数据源不存在");

            String fieldSort   = str(cfg.get("field_sort"));
            String fieldStatus = str(cfg.get("field_status"));
            String fieldParent = str(cfg.get("field_parent"));
            String mode        = str(cfg.getOrDefault("sync_mode", "level_diff"));

            // 构造查询 SQL
            String querySql = buildQuerySql(cfg, sqlMode, fieldCode, fieldName, fieldSort, fieldStatus, fieldParent);

            // 1) 拉取源数据
            boolean hasParent = !fieldParent.isBlank();
            List<Map<String, Object>> src = fetchRows(ds, querySql, fieldCode, fieldName,
                    fieldSort, fieldStatus, fieldParent, sqlMode);

            // 2) 层级推导
            int[] cum = hasParent ? new int[0] : cumulativeLengths(mapper.listLevelRules(enumId));
            Map<String, String> parentMap = new HashMap<>();
            Set<String> allCodes = new HashSet<>();
            for (Map<String, Object> r : src) allCodes.add(str(r.get("code")));
            if (hasParent) {
                for (Map<String, Object> r : src) {
                    String code = str(r.get("code"));
                    String p = r.get("parentRaw") == null ? "" : String.valueOf(r.get("parentRaw")).trim();
                    if (!p.isBlank() && !p.equals(code)) parentMap.put(code, p);
                }
            }

            // 3) 现有项（含锁定标记）
            List<Map<String, Object>> existing = mapper.listItems(enumId);
            Map<String, Map<String, Object>> byCode = new HashMap<>();
            Set<String> lockedCodes = new HashSet<>();
            for (Map<String, Object> e : existing) {
                byCode.put(str(e.get("code")), e);
                if (toInt(e.getOrDefault("is_sync_locked", 0)) == 1) {
                    lockedCodes.add(str(e.get("code")));
                }
            }

            boolean overwrite = "full_overwrite".equals(mode);
            if (overwrite) {
                // 全量覆盖：一次性删除该枚举下所有非锁定项
                del += mapper.deleteUnlockedItemsByEnum(enumId);
                // byCode 只保留锁定项
                byCode.entrySet().removeIf(en -> !lockedCodes.contains(en.getKey()));
            }

            // 4) upsert 源数据（批量收集新增项）
            List<Map<String, Object>> insertBatch = new ArrayList<>();
            Set<String> srcCodes = new HashSet<>();
            int seq = 0;
            for (Map<String, Object> r : src) {
                String code = str(r.get("code"));
                srcCodes.add(code);

                // 锁定项：跳过同步覆盖
                if (lockedCodes.contains(code)) { seq++; continue; }

                int level; String parent;
                if (hasParent) {
                    parent = parentMap.get(code);
                    level  = depthByParent(code, parentMap, allCodes);
                } else {
                    level  = levelOf(code, cum);
                    parent = parentOf(code, cum);
                }
                String status  = mapStatus(r.get("status"));
                int sortNum    = parseSort(r.get("sort"), seq++);

                Map<String, Object> ex = overwrite ? null : byCode.get(code);
                if (ex != null) {
                    Map<String, Object> u = new HashMap<>();
                    u.put("id",          ex.get("id"));
                    u.put("label",       r.get("label"));
                    u.put("api_name",    ex.get("api_name"));
                    u.put("parent_code", parent);
                    u.put("level",       level);
                    u.put("sort_num",    sortNum);
                    u.put("status",      status);
                    mapper.updateItem(u);
                    upd++;
                } else {
                    Map<String, Object> ins = new HashMap<>();
                    ins.put("id",          "enum-item-" + UUID.randomUUID());
                    ins.put("enum_id",     enumId);
                    ins.put("code",        code);
                    ins.put("api_name",    null);
                    ins.put("label",       r.get("label"));
                    ins.put("parent_code", parent);
                    ins.put("level",       level);
                    ins.put("sort_num",    sortNum);
                    ins.put("status",      status);
                    insertBatch.add(ins);
                    add++;
                    // 达到批次大小则提交
                    if (insertBatch.size() >= BATCH_SIZE) {
                        mapper.batchInsertItems(insertBatch);
                        insertBatch.clear();
                    }
                }
            }
            // 提交剩余批次
            if (!insertBatch.isEmpty()) {
                mapper.batchInsertItems(insertBatch);
            }

            // 5) level_diff: 删除源中已不存在的非锁定项
            if ("level_diff".equals(mode)) {
                for (Map<String, Object> e : existing) {
                    String code = str(e.get("code"));
                    if (!srcCodes.contains(code) && !lockedCodes.contains(code)) {
                        mapper.deleteItem(str(e.get("id")));
                        del++;
                    }
                }
            }

            log.put("add_count",    add);
            log.put("update_count", upd);
            log.put("del_count",    del);
            log.put("fail_count",   0);
            log.put("sync_status",  "success");
            log.put("error_msg",    null);
            mapper.insertSyncLog(log);
            return log;
        } catch (Exception e) {
            log.put("add_count",    add);
            log.put("update_count", upd);
            log.put("del_count",    del);
            log.put("fail_count",   0);
            log.put("sync_status",  "failed");
            log.put("error_msg",    e.getMessage());
            try { mapper.insertSyncLog(log); } catch (Exception ignore) {}
            log.put("_error", e.getMessage());
            return log;
        }
    }

    /**
     * 预览 SQL：执行自定义 SQL，返回列名 + 前5行数据，供前端字段映射下拉使用。
     */
    public Map<String, Object> previewSql(String dataSourceId, String sql) throws Exception {
        SysDataSource ds = dsService.get(dataSourceId);
        if (ds == null) throw new IllegalStateException("数据源不存在");
        String wrapped = "SELECT * FROM (" + sql.trim().replaceAll(";$", "") + ") __preview__ LIMIT 5";
        List<String> columns = new ArrayList<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = connector.open(ds);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(wrapped)) {
            ResultSetMetaData meta = rs.getMetaData();
            int n = meta.getColumnCount();
            for (int i = 1; i <= n; i++) columns.add(meta.getColumnLabel(i));
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (String col : columns) row.put(col, rs.getObject(col));
                rows.add(row);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("columns", columns);
        result.put("rows", rows);
        return result;
    }

    // —— 构造查询 SQL ——

    private String buildQuerySql(Map<String, Object> cfg, boolean sqlMode,
                                  String fieldCode, String fieldName,
                                  String fieldSort, String fieldStatus, String fieldParent) {
        if (sqlMode) {
            String customSql = str(cfg.get("custom_sql")).trim();
            if (customSql.isBlank()) throw new IllegalStateException("SQL 模式下未填写自定义 SQL");
            return customSql;
        }
        String table  = str(cfg.get("table_name")).trim();
        String filter = str(cfg.get("filter_sql")).trim();
        if (table.isBlank()) throw new IllegalStateException("表模式下未配置数据表");
        StringBuilder sql = new StringBuilder("SELECT ");
        sql.append(col(fieldCode)).append(" AS ec, ").append(col(fieldName)).append(" AS el");
        if (!fieldSort.isBlank())   sql.append(", ").append(col(fieldSort)).append(" AS es");
        if (!fieldStatus.isBlank()) sql.append(", ").append(col(fieldStatus)).append(" AS est");
        if (!fieldParent.isBlank()) sql.append(", ").append(col(fieldParent)).append(" AS ep");
        sql.append(" FROM ").append(table);
        if (!filter.isBlank()) sql.append(" WHERE ").append(filter);
        return sql.toString();
    }

    private List<Map<String, Object>> fetchRows(SysDataSource ds, String sql,
                                                 String fieldCode, String fieldName,
                                                 String fieldSort, String fieldStatus, String fieldParent,
                                                 boolean sqlMode) throws Exception {
        boolean hasSort   = !fieldSort.isBlank();
        boolean hasStatus = !fieldStatus.isBlank();
        boolean hasParent = !fieldParent.isBlank();
        List<Map<String, Object>> src = new ArrayList<>();
        try (Connection conn = connector.open(ds);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String code;
                String label;
                if (sqlMode) {
                    // SQL 模式：按配置的字段名直接取列
                    code  = rs.getString(fieldCode);
                    label = rs.getString(fieldName);
                } else {
                    code  = rs.getString("ec");
                    label = rs.getString("el");
                }
                if (code == null || code.isBlank()) continue;
                Map<String, Object> row = new HashMap<>();
                row.put("code",      code.trim());
                row.put("label",     label);
                row.put("sort",      hasSort   ? (sqlMode ? rs.getString(fieldSort)   : rs.getString("es"))  : null);
                row.put("status",    hasStatus ? (sqlMode ? rs.getString(fieldStatus) : rs.getString("est")) : null);
                row.put("parentRaw", hasParent ? (sqlMode ? rs.getString(fieldParent) : rs.getString("ep"))  : null);
                src.add(row);
            }
        }
        return src;
    }

    // —— 层次编码工具方法 ——

    private int[] cumulativeLengths(List<Map<String, Object>> rules) {
        if (rules == null || rules.isEmpty()) return new int[0];
        int[] cum = new int[rules.size()];
        int sum = 0, i = 0;
        for (Map<String, Object> r : rules) { sum += Math.max(0, toInt(r.get("code_len"))); cum[i++] = sum; }
        return cum;
    }

    private int levelOf(String code, int[] cum) {
        if (cum.length == 0 || code == null) return 1;
        int len = code.length();
        for (int i = 0; i < cum.length; i++) if (cum[i] == len) return i + 1;
        int lvl = 1;
        for (int i = 0; i < cum.length; i++) if (cum[i] <= len) lvl = i + 1;
        return lvl;
    }

    private String parentOf(String code, int[] cum) {
        if (cum.length == 0 || code == null) return null;
        int lvl = levelOf(code, cum);
        if (lvl <= 1) return null;
        int plen = cum[lvl - 2];
        return code.length() >= plen ? code.substring(0, plen) : null;
    }

    private int depthByParent(String code, Map<String, String> parentMap, Set<String> allCodes) {
        int d = 1; String cur = code; Set<String> seen = new HashSet<>(); seen.add(cur);
        while (true) {
            String p = parentMap.get(cur);
            if (p == null || p.isBlank() || seen.contains(p) || !allCodes.contains(p)) break;
            d++; seen.add(p); cur = p;
            if (d > 50) break;
        }
        return d;
    }

    private static String mapStatus(Object v) {
        if (v == null) return "active";
        String s = String.valueOf(v).trim().toLowerCase();
        if (s.isEmpty()) return "active";
        return Set.of("0","n","no","false","inactive","disabled","停用","禁用","无效").contains(s) ? "inactive" : "active";
    }

    private static int parseSort(Object v, int fallback) {
        if (v == null) return fallback;
        try { return (int) Double.parseDouble(String.valueOf(v).trim()); } catch (Exception e) { return fallback; }
    }

    private static String col(String ident) {
        return ident == null ? "" : ident.replaceAll("[^A-Za-z0-9_]", "");
    }

    private static boolean isBlank(Object o) { return o == null || String.valueOf(o).trim().isEmpty(); }
    private static String str(Object o) { return o == null ? "" : String.valueOf(o); }
    private static int toInt(Object o) {
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(o).trim()); } catch (Exception e) { return 0; }
    }
}
