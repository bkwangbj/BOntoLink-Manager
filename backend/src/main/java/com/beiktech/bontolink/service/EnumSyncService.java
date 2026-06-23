package com.beiktech.bontolink.service;

import com.beiktech.bontolink.common.DataSourceConnector;
import com.beiktech.bontolink.entity.SysDataSource;
import com.beiktech.bontolink.mapper.EnumTypeMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

/**
 * 枚举数据库同步:按 ont_enum_sync_config 连接真实数据源, 拉取目标表数据(字段映射 + 顶级筛选),
 * 写入 ont_enum_items。支持层次编码规则(按 code 长度推导 level / parent_code)与三种同步模式:
 *  - full_overwrite 全量覆盖: 先清空再插入
 *  - level_diff     逐级对比(以业务系统为准): upsert + 删除源中已不存在的项
 *  - incremental    增量更新: 仅新增/更新, 不删除
 * 注意: 故意不加 @Transactional —— 外部源库读取与本地写入分属两条连接, 且日志需在失败时落库。
 */
@Slf4j
@Service
public class EnumSyncService {

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
            if (cfg == null || isBlank(cfg.get("data_source_id")) || isBlank(cfg.get("table_name")))
                throw new IllegalStateException("未配置同步规则(数据源/数据表)");
            String fieldCode = str(cfg.get("field_code"));
            String fieldName = str(cfg.get("field_name"));
            if (fieldCode.isBlank() || fieldName.isBlank())
                throw new IllegalStateException("未配置编码字段/名称字段");

            SysDataSource ds = dsService.get(str(cfg.get("data_source_id")));
            if (ds == null) throw new IllegalStateException("数据源不存在");

            String fieldSort = str(cfg.get("field_sort"));
            String fieldStatus = str(cfg.get("field_status"));
            String table = str(cfg.get("table_name")).trim();
            String filter = str(cfg.get("filter_sql")).trim();
            String mode = str(cfg.getOrDefault("sync_mode", "level_diff"));

            StringBuilder sql = new StringBuilder("SELECT ");
            sql.append(col(fieldCode)).append(" AS ec, ").append(col(fieldName)).append(" AS el");
            if (!fieldSort.isBlank())   sql.append(", ").append(col(fieldSort)).append(" AS es");
            if (!fieldStatus.isBlank()) sql.append(", ").append(col(fieldStatus)).append(" AS est");
            sql.append(" FROM ").append(table);
            if (!filter.isBlank()) sql.append(" WHERE ").append(filter);

            // 1) 拉取源数据
            List<Map<String, Object>> src = new ArrayList<>();
            try (Connection conn = connector.open(ds);
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sql.toString())) {
                boolean hasSort = !fieldSort.isBlank(), hasStatus = !fieldStatus.isBlank();
                while (rs.next()) {
                    String code = rs.getString("ec");
                    if (code == null || code.isBlank()) continue;
                    Map<String, Object> row = new HashMap<>();
                    row.put("code", code.trim());
                    row.put("label", rs.getString("el"));
                    row.put("sort", hasSort ? rs.getString("es") : null);
                    row.put("status", hasStatus ? rs.getString("est") : null);
                    src.add(row);
                }
            }

            // 2) 层次编码规则 → 按 code 长度推导 level / parent_code (无规则则平铺为一级)
            int[] cum = cumulativeLengths(mapper.listLevelRules(enumId));

            // 3) 现有项
            List<Map<String, Object>> existing = mapper.listItems(enumId);
            Map<String, Map<String, Object>> byCode = new HashMap<>();
            for (Map<String, Object> e : existing) byCode.put(str(e.get("code")), e);

            boolean overwrite = "full_overwrite".equals(mode);
            if (overwrite) {
                for (Map<String, Object> e : existing) { mapper.deleteItem(str(e.get("id"))); del++; }
                byCode.clear();
            }

            // 4) upsert 源数据
            Set<String> srcCodes = new HashSet<>();
            int seq = 0;
            for (Map<String, Object> r : src) {
                String code = str(r.get("code"));
                srcCodes.add(code);
                int level = levelOf(code, cum);
                String parent = parentOf(code, cum);
                String status = mapStatus(r.get("status"));
                int sortNum = parseSort(r.get("sort"), seq++);

                Map<String, Object> ex = overwrite ? null : byCode.get(code);
                if (ex != null) {
                    Map<String, Object> u = new HashMap<>();
                    u.put("id", ex.get("id"));
                    u.put("label", r.get("label"));
                    u.put("api_name", ex.get("api_name"));
                    u.put("parent_code", parent);
                    u.put("level", level);
                    u.put("sort_num", sortNum);
                    u.put("status", status);
                    mapper.updateItem(u);
                    upd++;
                } else {
                    Map<String, Object> ins = new HashMap<>();
                    ins.put("id", "enum-item-" + UUID.randomUUID());
                    ins.put("enum_id", enumId);
                    ins.put("code", code);
                    ins.put("api_name", null);
                    ins.put("label", r.get("label"));
                    ins.put("parent_code", parent);
                    ins.put("level", level);
                    ins.put("sort_num", sortNum);
                    ins.put("status", status);
                    mapper.insertItem(ins);
                    add++;
                }
            }

            // 5) level_diff: 以业务系统为准, 删除源中已不存在的项
            if ("level_diff".equals(mode)) {
                for (Map<String, Object> e : existing) {
                    if (!srcCodes.contains(str(e.get("code")))) { mapper.deleteItem(str(e.get("id"))); del++; }
                }
            }

            log.put("add_count", add);
            log.put("update_count", upd);
            log.put("del_count", del);
            log.put("fail_count", 0);
            log.put("sync_status", "success");
            log.put("error_msg", null);
            mapper.insertSyncLog(log);
            return log;
        } catch (Exception e) {
            log.put("add_count", add);
            log.put("update_count", upd);
            log.put("del_count", del);
            log.put("fail_count", 0);
            log.put("sync_status", "failed");
            log.put("error_msg", e.getMessage());
            try { mapper.insertSyncLog(log); } catch (Exception ignore) { /* 日志落库失败不掩盖原始错误 */ }
            log.put("_error", e.getMessage());
            return log;
        }
    }

    /* —— 层次编码: 各层累计 code 长度 —— */
    private int[] cumulativeLengths(List<Map<String, Object>> rules) {
        if (rules == null || rules.isEmpty()) return new int[0];
        int[] cum = new int[rules.size()];
        int sum = 0, i = 0;
        for (Map<String, Object> r : rules) {
            sum += Math.max(0, toInt(r.get("code_len")));
            cum[i++] = sum;
        }
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

    /* —— 状态映射: 源值 → active/inactive —— */
    private static String mapStatus(Object v) {
        if (v == null) return "active";
        String s = String.valueOf(v).trim().toLowerCase();
        if (s.isEmpty()) return "active";
        Set<String> off = Set.of("0", "n", "no", "false", "inactive", "disabled", "停用", "禁用", "无效");
        return off.contains(s) ? "inactive" : "active";
    }
    private static int parseSort(Object v, int fallback) {
        if (v == null) return fallback;
        try { return (int) Double.parseDouble(String.valueOf(v).trim()); }
        catch (Exception e) { return fallback; }
    }
    /* 列名白名单化, 防注入(列来自元数据选择, 仅允许字母数字下划线) */
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
