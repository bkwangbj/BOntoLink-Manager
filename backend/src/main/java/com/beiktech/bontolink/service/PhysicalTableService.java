package com.beiktech.bontolink.service;

import com.beiktech.bontolink.mapper.PhysicalTableMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 物理表/视图元数据的落库与同步。
 * 同步源为后端自身库(通过 JDBC DatabaseMetaData 读取, 方言无关);
 * 同步时只更新结构(类型/字段), 保留用户设置的中文名(display_name)。
 */
@Service
public class PhysicalTableService {

    @Autowired private DataSource dataSource;
    @Autowired private PhysicalTableMapper mapper;
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
        List<Map<String, Object>> current = readFromDatabase();
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
            Map<String, Object> old = existing.get(name);
            if (old != null) {
                Map<String, Object> upd = new HashMap<>();
                upd.put("id", old.get("id"));
                upd.put("tableType", t.get("type"));
                upd.put("columnsJson", columnsJson);
                upd.put("columnCount", t.get("column_count"));
                upd.put("syncTime", now);
                upd.put("updateTime", now);
                mapper.updateStructure(upd);   // 不动 display_name
            } else {
                Map<String, Object> ins = new HashMap<>();
                ins.put("id", "phys-" + UUID.randomUUID());
                ins.put("dsId", dsId);
                ins.put("physicalTable", name);
                ins.put("displayName", name);  // 新表中文名默认= 物理名, 之后用户可改
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

    private String toJson(Object v) {
        try { return json.writeValueAsString(v == null ? Collections.emptyList() : v); }
        catch (Exception e) { return "[]"; }
    }

    /** 通过 JDBC DatabaseMetaData 读取后端自身库的所有 table / view */
    private List<Map<String, Object>> readFromDatabase() {
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            String catalog = conn.getCatalog();

            List<String[]> tables = new ArrayList<>();   // [name, type]
            try (ResultSet rs = meta.getTables(catalog, null, "%", new String[]{"TABLE", "VIEW"})) {
                while (rs.next()) {
                    String name = rs.getString("TABLE_NAME");
                    if (isInfraTable(name)) continue;
                    tables.add(new String[]{name, rs.getString("TABLE_TYPE")});
                }
            }
            for (String[] t : tables) {
                String name = t[0];
                boolean isView = t[1] != null && t[1].toUpperCase().contains("VIEW");
                List<Map<String, Object>> columns = new ArrayList<>();
                try (ResultSet cr = meta.getColumns(catalog, null, name, "%")) {
                    while (cr.next()) {
                        Map<String, Object> c = new LinkedHashMap<>();
                        c.put("name", cr.getString("COLUMN_NAME"));
                        c.put("type", mapType(cr.getString("TYPE_NAME")));
                        columns.add(c);
                    }
                }
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("physical_table", name);
                row.put("type", isView ? "view" : "table");
                row.put("columns", columns);
                row.put("column_count", columns.size());
                result.add(row);
            }
        } catch (SQLException e) {
            throw new RuntimeException("读取物理表失败: " + e.getMessage(), e);
        }
        return result;
    }

    private static boolean isInfraTable(String name) {
        if (name == null) return true;
        String n = name.toLowerCase();
        return n.startsWith("sqlite_") || n.equals("flyway_schema_history") || n.equals("ont_physical_table");
    }

    /** JDBC 类型名 → 前端简单类型 */
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
