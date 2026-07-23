package com.beiktech.bontolink.service;

import com.beiktech.bontolink.common.DataSourceConnector;
import com.beiktech.bontolink.data.entity.SysDataSource;
import com.beiktech.bontolink.data.mapper.PhysicalTableMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 物理表/视图元数据的落库与同步。
 * 同步源为该数据源自身配置的库(按 dsId 取 sys_data_source 配置, 动态 JDBC 连接读取 DatabaseMetaData, 方言无关);
 * 同步时只更新结构(类型/字段), 保留用户设置的中文名(display_name)。
 */
@Slf4j
@Service
public class PhysicalTableService {

    @Autowired private DataSourceService dsService;
    @Autowired private PhysicalTableMapper mapper;
    @Autowired private DataSourceConnector connector;
    private final ObjectMapper json = new ObjectMapper();
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 列出某数据源已落库的物理表(dsId 为空则全部), 并把 columns_json 解析为 columns 数组 */
    public List<Map<String, Object>> list(String dsId) {
        List<Map<String, Object>> rows = (dsId == null || dsId.isEmpty())
                ? mapper.listAll() : mapper.listByDs(dsId);
        for (Map<String, Object> r : rows) enrich(r);
        return rows;
    }

    /** 同步: 读自身库现状, 新增插入、已存在仅更新结构(保留中文名)、库中已删除的清理掉 */
    public List<Map<String, Object>> sync(String dsId) {
        if (dsId == null || dsId.isEmpty()) throw new IllegalArgumentException("缺少数据源 dsId");
        SysDataSource ds = dsService.get(dsId);
        if (ds == null) throw new IllegalArgumentException("数据源不存在");
        List<Map<String, Object>> current = readFromDatabase(ds);
        String now = LocalDateTime.now().format(TS);

        // 已存在: physical_table -> 行
        Map<String, Map<String, Object>> existing = new LinkedHashMap<>();
        for (Map<String, Object> r : mapper.listByDs(dsId)) {
            existing.put(String.valueOf(r.get("physical_table")), r);
        }
        Set<String> currentNames = new HashSet<>();

        for (Map<String, Object> t : current) {
            String name = String.valueOf(t.get("physical_table"));
            currentNames.add(name);
            String columnsJson = toJson(t.get("columns"));
            // 表注释(REMARKS); 同步规则: 有注释→中文名用注释, 无注释→新表用物理名/旧表保留原名
            String remark = t.get("display_name") == null ? "" : t.get("display_name").toString().trim();
            Map<String, Object> old = existing.get(name);
            if (old != null) {
                Map<String, Object> upd = new HashMap<>();
                upd.put("id", old.get("id"));
                upd.put("tableType", t.get("type"));
                upd.put("columnsJson", columnsJson);
                upd.put("columnCount", t.get("column_count"));
                upd.put("syncTime", now);
                upd.put("updateTime", now);
                mapper.updateStructure(upd);   // 结构字段; display_name 单独按下方规则处理
                // 有注释则把注释刷进中文名(重新同步也能更新); 无注释不动, 避免覆盖用户手改的名字
                String oldDisp = old.get("display_name") == null ? "" : old.get("display_name").toString();
                if (!remark.isEmpty() && !remark.equals(oldDisp)) {
                    mapper.updateDisplayName(String.valueOf(old.get("id")), remark, now);
                }
            } else {
                Map<String, Object> ins = new HashMap<>();
                ins.put("id", "phys-" + UUID.randomUUID());
                ins.put("dsId", dsId);
                ins.put("physicalTable", name);
                // 新表中文名: 有注释用注释, 无注释退回物理表名(之后用户可改)
                ins.put("displayName", remark.isEmpty() ? name : remark);
                ins.put("tableType", t.get("type"));
                ins.put("columnsJson", columnsJson);
                ins.put("columnCount", t.get("column_count"));
                ins.put("syncTime", now);
                mapper.insert(ins);
            }
        }
        // 清理库中已不存在的表
        for (Map.Entry<String, Map<String, Object>> e : existing.entrySet()) {
            if (!currentNames.contains(e.getKey())) {
                mapper.deleteById(String.valueOf(e.getValue().get("id")));
            }
        }
        return list(dsId);
    }

    /** 按 id 删除物理表记录（不影响远端实际数据库结构）。 */
    public void delete(String id) {
        mapper.deleteById(id);
    }

    /** 修改中文名 */
    public Map<String, Object> updateName(String id, String displayName) {
        mapper.updateDisplayName(id, displayName, LocalDateTime.now().format(TS));
        Map<String, Object> r = mapper.findById(id);
        if (r != null) enrich(r);
        return r;
    }

    /** 把 columns_json 解析回 columns 数组, 并补充前端用的 type 别名 */
    @SuppressWarnings("unchecked")
    private void enrich(Map<String, Object> r) {
        Object cj = r.get("columns_json");
        List<Object> cols = new ArrayList<>();
        if (cj != null) {
            try { cols = json.readValue(cj.toString(), List.class); } catch (Exception ignore) {}
        }
        r.put("columns", cols);
        r.put("type", r.get("table_type"));
    }

    /** 将对象序列化为 JSON 字符串，失败时返回 "[]" 防止列字段为 null。 */
    private String toJson(Object v) {
        try { return json.writeValueAsString(v == null ? Collections.emptyList() : v); }
        catch (Exception e) { return "[]"; }
    }

    /** 按数据源配置动态建立 JDBC 连接, 读取其库中所有 table / view */
    private List<Map<String, Object>> readFromDatabase(SysDataSource ds) {
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = connector.open(ds)) {
            DatabaseMetaData meta = conn.getMetaData();
            String catalog = conn.getCatalog();

            List<String[]> tables = new ArrayList<>();   // [name, type, schema, remarks]
            try (ResultSet rs = meta.getTables(catalog, null, "%", new String[]{"TABLE", "VIEW"})) {
                while (rs.next()) {
                    String name = rs.getString("TABLE_NAME");
                    String schema = rs.getString("TABLE_SCHEM");
                    String nl = name == null ? "" : name.toLowerCase();
                    // 跳过通用基础设施表
                    if (nl.isEmpty() || nl.startsWith("sqlite_")
                            || nl.equals("flyway_schema_history") || nl.equals("ont_physical_table")) continue;
                    if (isSystemSchema(schema)) continue;   // 跳过 pg_catalog / information_schema 等系统库
                    tables.add(new String[]{name, rs.getString("TABLE_TYPE"), schema, rs.getString("REMARKS")});
                }
            }
            for (String[] t : tables) {
                String name = t[0];
                String schema = t[2];
                String tableRemarks = t[3];   // 表注释
                boolean isView = t[1] != null && t[1].toUpperCase().contains("VIEW");
                // 主键列集合 (视图无主键, 容错忽略异常)
                Set<String> pkCols = new HashSet<>();
                try (ResultSet pk = meta.getPrimaryKeys(catalog, schema, name)) {
                    while (pk.next()) pkCols.add(pk.getString("COLUMN_NAME"));
                } catch (SQLException ignore) {}
                List<Map<String, Object>> columns = new ArrayList<>();
                try (ResultSet cr = meta.getColumns(catalog, schema, name, "%")) {
                    while (cr.next()) {
                        Map<String, Object> c = new LinkedHashMap<>();
                        String colName = cr.getString("COLUMN_NAME");
                        c.put("name", colName);
                        c.put("type", mapType(cr.getString("TYPE_NAME")));
                        String remarks = cr.getString("REMARKS");
                        c.put("comment", remarks == null ? "" : remarks);   // 字段注释
                        // NOT NULL → 必填; columnNoNulls 表示不允许 NULL
                        c.put("is_required", cr.getInt("NULLABLE") == DatabaseMetaData.columnNoNulls ? 1 : 0);
                        c.put("is_key", pkCols.contains(colName) ? 1 : 0);   // 主键标识
                        columns.add(c);
                    }
                }
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("physical_table", name);
                row.put("type", isView ? "view" : "table");
                row.put("display_name", tableRemarks == null ? "" : tableRemarks.trim());   // 表注释 → 中文名
                row.put("columns", columns);
                row.put("column_count", columns.size());
                result.add(row);
            }
        } catch (SQLException e) {
            log.error("连接数据源失败: " + e.getMessage(), e);
            throw new RuntimeException("连接数据源失败: " + e.getMessage(), e);
        }
        return result;
    }

    /** 系统库/模式, 不纳入物理表清单 */
    private static boolean isSystemSchema(String schema) {
        if (schema == null) return false;
        String s = schema.toLowerCase();
        return s.equals("pg_catalog") || s.equals("information_schema")
                || s.equals("mysql") || s.equals("performance_schema") || s.equals("sys")
                || s.startsWith("pg_");
    }

    /**
     * 将 JDBC 原始类型名（如 "varchar2"、"int4"、"timestamptz"）映射为前端统一简单类型。
     * 未匹配的类型一律归为 "string"。
     */
    private static String mapType(String typeName) {
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
}
