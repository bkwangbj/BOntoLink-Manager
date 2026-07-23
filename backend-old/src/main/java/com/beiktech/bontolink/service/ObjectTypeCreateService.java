package com.beiktech.bontolink.service;

import com.beiktech.bontolink.data.mapper.ClassMetaMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 对象类型一站式创建：根据新建向导收集的(数据源/主表/附表/字段映射)，
 * 事务化写入 ont_class + ont_class_ds(主表/附表) + ont_class_property(字段映射)。
 */
@Service
public class ObjectTypeCreateService {

    @Autowired private ClassMetaMapper mapper;
    private final ObjectMapper json = new ObjectMapper();

    /**
     * 事务化创建一个对象类型（包含数据源绑定与字段映射）。
     *
     * @param body 向导提交的数据，包含：
     *             api_name / display_name / category_code / ns_code / icon / color / description —— 对象基本信息；
     *             ds_code —— 数据源编码；
     *             main —— 主表配置（physical_table / alias_name / pk_keys）；
     *             subs —— 附表列表（physical_table / alias_name / join_on_keys / join_type）；
     *             props —— 字段映射列表（physical_table / physical_column / api_name / data_type 等）。
     * @return 已插入的 ont_class 行（Map 形式）
     */
    @Transactional
    public Map<String, Object> create(Map<String, Object> body) {
        // 1) 对象主表 ont_class
        String base = sanitize(str(body.get("api_name")));
        if (base.isEmpty()) base = "object_type";
        String apiName = uniqueApiName(base);
        String classId = "class-" + UUID.randomUUID();
        String displayName = firstNonBlank(str(body.get("display_name")), apiName);

        // 命名空间:向导未指定时,按所属领域(category)自动带入(领域未绑定则向上回溯到行业)
        String nsCode = str(body.get("ns_code"));
        if (nsCode.isBlank()) nsCode = resolveNsCode(str(body.get("category_code")));

        Map<String, Object> cls = new HashMap<>();
        cls.put("id", classId);
        cls.put("rid", "ri.ont.class." + classId);
        cls.put("api_name", apiName);
        cls.put("ns_code", (nsCode == null || nsCode.isBlank()) ? null : nsCode);
        cls.put("category_code", body.get("category_code"));
        cls.put("display_name", displayName);
        cls.put("rdfs_label", firstNonBlank(str(body.get("rdfs_label")), displayName));
        cls.put("rdfs_comment", body.get("description"));
        cls.put("rdfs_see_also", null);
        cls.put("rdfs_defined_by", null);
        cls.put("description", body.get("description"));
        cls.put("icon", firstNonBlank(str(body.get("icon")), "cube"));
        cls.put("color", body.get("color"));
        cls.put("status", 1);
        cls.put("metadata", null);
        cls.put("parent_class_id", null);
        cls.put("class_expr_type", null);
        cls.put("class_expr_content", null);
        cls.put("is_thing", 0);
        cls.put("is_nothing", 0);
        cls.put("is_common", 0);
        mapper.insertClass(cls);

        // 2) 数据源绑定 + 字段映射（仅"使用已有数据源"模式）
        String dsCode = str(body.get("ds_code"));
        Map<String, Object> main = asMap(body.get("main"));
        List<Map<String, Object>> subs = asList(body.get("subs"));
        List<Map<String, Object>> props = asList(body.get("props"));

        // 字段按物理表分组（用于 physical_fields）
        Map<String, List<Map<String, Object>>> propsByTable = new LinkedHashMap<>();
        for (Map<String, Object> p : props) {
            propsByTable.computeIfAbsent(str(p.get("physical_table")), k -> new ArrayList<>()).add(p);
        }

        Map<String, String> dsIdByTable = new HashMap<>();
        Set<String> mainPk = new HashSet<>();
        String mainTable = null;

        if (main != null && main.get("physical_table") != null) {
            mainTable = str(main.get("physical_table"));
            for (Object k : asList(main.get("pk_keys"))) { if (k != null) mainPk.add(String.valueOf(k)); }
            String dsRowId = insertDs(classId, dsCode, mainTable, str(main.get("alias_name")),
                    1, "main", joinCsv(main.get("pk_keys")), null, null, propsByTable.get(mainTable), mainPk, 0);
            dsIdByTable.put(mainTable, dsRowId);

            int si = 1;
            for (Map<String, Object> sub : subs) {
                String st = str(sub.get("physical_table"));
                String dsRowId2 = insertDs(classId, dsCode, st, str(sub.get("alias_name")),
                        2, "s" + si, null, str(sub.get("join_on_keys")),
                        firstNonBlank(str(sub.get("join_type")), "LEFT"), propsByTable.get(st), Collections.emptySet(), si);
                dsIdByTable.put(st, dsRowId2);
                si++;
            }
        }

        // 3) 字段 → ont_class_property
        int sort = 0;
        for (Map<String, Object> p : props) {
            String pt = str(p.get("physical_table"));
            String col = str(p.get("physical_column"));
            String pApi = sanitize(str(p.get("api_name")));
            if (pApi.isEmpty()) pApi = sanitize(col);
            boolean isPk = pt.equals(mainTable) && mainPk.contains(col);

            String pid = "class-property-" + UUID.randomUUID();
            Map<String, Object> row = new HashMap<>();
            row.put("id", pid);
            row.put("rid", "ri.ont.class.property." + pid);
            row.put("class_id", classId);
            row.put("category_code", body.get("category_code"));
            row.put("api_name", pApi);
            row.put("prop_code", pApi);
            row.put("prop_type", "data");
            row.put("data_type", p.get("data_type"));
            row.put("value_type", null);
            row.put("display_name", firstNonBlank(str(p.get("display_name")), pApi));
            row.put("rdfs_label", firstNonBlank(str(p.get("display_name")), pApi));
            row.put("class_ds_id", dsIdByTable.get(pt));
            row.put("physical_table", pt);
            row.put("physical_column", col);
            row.put("is_primary", isPk ? 1 : 0);
            row.put("is_required", isPk ? 1 : toInt(p.get("is_required")));
            row.put("is_key", toInt(p.get("is_key")));
            row.put("is_derived", 0);
            row.put("is_multi_valued_prop", toInt(p.get("is_multi_valued_prop")));
            row.put("is_range_constraint_prop", toInt(p.get("is_range_constraint_prop")));
            setFlagDefaults(row);
            row.put("sort", sort++);
            row.put("status", 1);
            mapper.insertClassProperty(row);
        }

        return cls;
    }

    /**
     * 向 ont_class_ds 写入一条数据源绑定记录，返回新行 id。
     *
     * @param classId       所属对象类型 ID
     * @param dsCode        数据源编码
     * @param physicalTable 物理表名
     * @param tableLabel    表别名（展示用）
     * @param relType       关系类型（1=主表 / 2=附表）
     * @param alias         SQL 别名（主表 "main"，附表 "s1"/"s2" 等）
     * @param pkKeys        主键列名逗号分隔（仅主表有效）
     * @param joinOnKeys    附表关联条件（仅附表有效）
     * @param joinType      JOIN 类型（"LEFT"/"INNER" 等，仅附表有效）
     * @param cols          该表所有字段映射列表（用于 physical_fields JSON）
     * @param pkSet         主键列名集合（标记 physical_fields 中哪些是主键）
     * @param sort          表顺序（主表 0，附表依次递增）
     * @return 新插入行的 id（格式 "class-ds-{UUID}"）
     */
    private String insertDs(String classId, String dsCode, String physicalTable, String tableLabel,
                            int relType, String alias, String pkKeys, String joinOnKeys, String joinType,
                            List<Map<String, Object>> cols, Set<String> pkSet, int sort) {
        String id = "class-ds-" + UUID.randomUUID();
        Map<String, Object> row = new HashMap<>();
        row.put("id", id);
        row.put("class_id", classId);
        row.put("ds_code", dsCode);
        row.put("physical_table", physicalTable);
        row.put("table_label", tableLabel);
        row.put("rel_type", relType);
        row.put("alias", alias);
        row.put("pk_keys", pkKeys);
        row.put("join_on_keys", joinOnKeys);
        row.put("join_type", joinType);
        row.put("physical_fields", toFieldsJson(cols, pkSet));
        row.put("sort", sort);
        row.put("status", 1);
        mapper.insertClassDs(row);
        return id;
    }

    /**
     * 将字段列表序列化为 physical_fields JSON 字符串（存入 ont_class_ds）。
     * 每个元素格式：{"name": "col", "data_type": "varchar", "is_pk": 0/1}。
     * 序列化失败时返回 "[]" 而不抛出异常，避免中断整体事务。
     */
    private String toFieldsJson(List<Map<String, Object>> cols, Set<String> pkSet) {
        List<Map<String, Object>> arr = new ArrayList<>();
        if (cols != null) {
            for (Map<String, Object> c : cols) {
                String name = str(c.get("physical_column"));
                Map<String, Object> f = new LinkedHashMap<>();
                f.put("name", name);
                f.put("data_type", c.get("data_type"));
                f.put("is_pk", pkSet != null && pkSet.contains(name) ? 1 : 0);
                arr.add(f);
            }
        }
        try { return json.writeValueAsString(arr); } catch (Exception e) { return "[]"; }
    }

    /** 按领域 category_code 解析命名空间编码:自身未绑定则沿 parent_id 向上回溯到行业 */
    private String resolveNsCode(String categoryCode) {
        if (categoryCode == null || categoryCode.isBlank()) return null;
        Map<String, Object> row = mapper.findCategoryByCode(categoryCode);
        int guard = 0;
        while (row != null && guard++ < 20) {
            String ns = str(row.get("ns_code"));
            if (!ns.isBlank()) return ns;
            String parentId = str(row.get("parent_id"));
            if (parentId.isBlank() || "0".equals(parentId)) break;
            row = mapper.findCategoryById(parentId);
        }
        return null;
    }

    /**
     * 生成全局唯一的 api_name：若 base 已存在则追加 _2、_3 …，直到找到空闲名称。
     */
    private String uniqueApiName(String base) {
        String name = base;
        int n = 2;
        while (mapper.existsApiName(name) > 0) {
            name = base + "_" + n++;
        }
        return name;
    }

    /** 将 OWL 语义标志（functional/transitive/symmetric 等）默认置 0，避免漏填字段导致 DB 约束报错。 */
    private static void setFlagDefaults(Map<String, Object> row) {
        for (String k : new String[]{
                "owl_functional", "owl_inverse_functional", "owl_transitive", "owl_symmetric",
                "owl_asymmetric", "owl_reflexive", "owl_irreflexive"}) {
            row.put(k, 0);
        }
    }

    /* —— 小工具 —— */
    private static String str(Object o) { return o == null ? "" : String.valueOf(o); }

    /** 返回 a（非空白时）否则 b，用于展示名称 / rdfs_label 兜底。 */
    private static String firstNonBlank(String a, String b) { return (a != null && !a.isBlank()) ? a : b; }

    /** 将任意字符串转为合法 api_name：非字母数字下划线替换为 _，整体小写。 */
    private static String sanitize(String s) {
        if (s == null) return "";
        return s.trim().replaceAll("[^A-Za-z0-9_]", "_").toLowerCase();
    }

    /** 将布尔/数字/字符串统一转为 0/1 整数标志（"true"/"1" → 1，其余 → 0）。 */
    private static int toInt(Object o) {
        if (o == null) return 0;
        if (o instanceof Boolean b) return b ? 1 : 0;
        if (o instanceof Number n) return n.intValue() != 0 ? 1 : 0;
        String s = String.valueOf(o).trim();
        return ("1".equals(s) || "true".equalsIgnoreCase(s)) ? 1 : 0;
    }

    /** 将列表或单个值拼成逗号分隔字符串（pk_keys 存储用）；列表为空时返回 null。 */
    private static String joinCsv(Object o) {
        if (o instanceof List<?> list) {
            StringJoiner sj = new StringJoiner(",");
            for (Object x : list) { if (x != null && !String.valueOf(x).isBlank()) sj.add(String.valueOf(x)); }
            return sj.length() == 0 ? null : sj.toString();
        }
        return o == null ? null : String.valueOf(o);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : null;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> asList(Object o) {
        return o instanceof List ? (List<Map<String, Object>>) o : Collections.emptyList();
    }
}
