package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.data.mapper.OntologyMapper;
import com.beiktech.bontolink.service.InstanceMockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 本体业务数据查询接口 —— 平台(hermes)端 /api/ontology/query 的真实取数通道。
 *
 * <p>职责(边界清晰):只做「按类取实例原始行 + 列定义投影」,<b>不做</b> 过滤/分组/聚合/排序/分页
 * (这些由平台端 QueryExecutor 原位处理)。数据来源为 {@link InstanceMockService}
 * 的内存确定性生成实例(每类 18~107 条),非真实库表;本接口仅作为统一数据通道暴露。</p>
 *
 * <pre>
 * GET /api/ontology/data?className=t1901_s01_1&properties=tb_un,dwqk12
 * → { className, classId, label, columns:[{field,label,dataType}], rows:[{field:value}], total }
 * </pre>
 */
@RestController
@RequestMapping("/api/ontology")
public class OntologyDataController {

    @Autowired private OntologyMapper ontologyMapper;
    @Autowired private InstanceMockService mock;

    private static final Set<String> IGNORED_SYS_COLUMNS = Set.of("id", "classId", "className", "classApiName",
            "icon", "color", "categoryCode", "code", "title", "rid", "createdAt");

    /** 按 className(api_name) 取本体实例原始数据 + 列定义。 */
    @GetMapping("/data")
    public R<Map<String, Object>> data(@RequestParam String className,
                                       @RequestParam(required = false) String properties) {
        Map<String, Object> cls = ontologyMapper.findClassByApiName(className);
        if (cls == null || cls.get("id") == null) {
            return R.error(400, "本体类不存在: " + className);
        }
        String classId = String.valueOf(cls.get("id"));
        String label = firstNonBlank(cls.get("display_name"), cls.get("rdfs_label"), className);

        // 属性元数据 → 列定义 (api_name → field, display_name → label, data_type 归一化)
        List<Map<String, Object>> propMeta = mock.properties(classId);
        List<Map<String, Object>> allColumns = new ArrayList<>();
        for (Map<String, Object> p : propMeta) {
            String field = str(p.get("api_name"));
            if (field.isEmpty() || IGNORED_SYS_COLUMNS.contains(field)) continue;
            Map<String, Object> col = new LinkedHashMap<>();
            col.put("field", field);
            col.put("label", firstNonBlank(p.get("display_name"), p.get("rdfs_label"), field));
            col.put("dataType", normType(str(p.get("data_type"))));
            allColumns.add(col);
        }

        // 投影白名单(可空 = 全部)
        Set<String> wanted = properties == null || properties.isBlank()
                ? null
                : new LinkedHashSet<>(Arrays.asList(properties.split("\\s*,\\s*")));

        List<Map<String, Object>> columns = new ArrayList<>();
        if (wanted == null) {
            columns = allColumns;
        } else {
            for (Map<String, Object> col : allColumns) {
                if (wanted.contains(String.valueOf(col.get("field")))) {
                    columns.add(col);
                }
            }
        }

        // 全部实例行(行 key = 属性 api_name),total = 投影前全量
        // 挂了外部主表的类必读真实库:取数失败(异常)时明确报错, 不降级 mock
        List<Map<String, Object>> rows;
        try {
            rows = mock.all(classId);
        } catch (IllegalStateException e) {
            return R.error(400, "外部库取数失败: " + e.getMessage());
        }
        int total = rows.size();
        // 投影:properties 为空 → 仅保留属性列(剔除系统列);指定 → 仅保留选中属性列
        Set<String> keep = new LinkedHashSet<>();
        for (Map<String, Object> col : allColumns) {
            keep.add(String.valueOf(col.get("field")));
        }
        if (wanted != null) {
            keep.retainAll(wanted);
        }
        List<Map<String, Object>> projected = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (String f : keep) {
                out.put(f, row.get(f));
            }
            projected.add(out);
        }
        rows = projected;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("className", className);
        data.put("classId", classId);
        data.put("label", label);
        data.put("columns", columns);
        data.put("rows", rows);
        data.put("total", total);
        return R.ok(data);
    }

    private static String normType(String dt) {
        if (dt == null) return "string";
        String d = dt.toLowerCase();
        if (d.contains("datetime") || d.contains("timestamp")) return "datetime";
        if (d.contains("bool")) return "boolean";
        if (d.equals("date") || d.startsWith("xsd:date")) return "date";
        if (d.contains("int") || d.contains("long") || d.contains("bigint")) return "int";
        if (d.contains("decimal") || d.contains("double") || d.contains("float") || d.contains("numeric")) return "decimal";
        return "string";
    }

    private static String str(Object o) { return o == null ? "" : String.valueOf(o); }

    private static String firstNonBlank(Object... ss) {
        for (Object s : ss) if (s != null && !String.valueOf(s).isBlank()) return String.valueOf(s);
        return "";
    }
}
