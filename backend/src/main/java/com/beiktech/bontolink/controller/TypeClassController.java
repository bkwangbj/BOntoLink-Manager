package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.mapper.TypeClassBindMapper;
import com.beiktech.bontolink.mapper.TypeClassCategoryMapper;
import com.beiktech.bontolink.mapper.TypeClassMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 类型类标准定义 REST 接口。路径:/api/type-classes
 * 升级版:本表仅"定义",绑定走 /api/tc-bind。
 */
@RestController
@RequestMapping("/api/type-classes")
public class TypeClassController {

    @Autowired private TypeClassMapper mapper;
    @Autowired private TypeClassCategoryMapper categoryMapper;
    @Autowired private TypeClassBindMapper bindMapper;
    private final ObjectMapper om = new ObjectMapper();
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String now() { return LocalDateTime.now().format(TS); }

    private List<String> jsonList(Object v) {
        if (v instanceof List<?> l) { List<String> r = new ArrayList<>(); for (Object o : l) r.add(String.valueOf(o)); return r; }
        if (v instanceof String s && !s.isBlank()) {
            try { return om.readValue(s, new TypeReference<List<String>>() {}); } catch (Exception ignore) {}
        }
        return new ArrayList<>();
    }

    private String toJsonStr(Object v) {
        if (v == null) return null;
        if (v instanceof String) return (String) v;
        try { return om.writeValueAsString(v); } catch (Exception e) { return null; }
    }

    /**
     * 列表(全量,不分页)。前端按需筛选;后端支持参数过滤。
     * @param categoryCode  大类编码
     * @param applicableType property/relation/action(按 allow_apply_types 包含)
     * @param deprecated    0 可用 / 1 已弃用
     * @param q             模糊(种类/名称/说明)
     */
    @GetMapping
    public R<List<Map<String, Object>>> list(
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) String applicableType,
            @RequestParam(required = false) Integer deprecated,
            @RequestParam(required = false) String q) {
        List<Map<String, Object>> all = mapper.listAll();
        String kw = q == null ? null : q.trim().toLowerCase();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> r : all) {
            if (categoryCode != null && !categoryCode.isBlank() && !categoryCode.equals(r.get("category_code"))) continue;
            if (deprecated != null && deprecated != toInt(r.get("is_deprecated"))) continue;
            if (applicableType != null && !applicableType.isBlank() && !jsonList(r.get("allow_apply_types")).contains(applicableType)) continue;
            if (kw != null && !kw.isEmpty()) {
                String hay = (str(r.get("category_code")) + " " + str(r.get("name_prefix")) + " "
                        + str(r.get("name_cn_base")) + " " + str(r.get("description"))).toLowerCase();
                if (!hay.contains(kw)) continue;
            }
            out.add(r);
        }
        return R.ok(out);
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> get(@PathVariable String id) { return R.ok(mapper.findById(id)); }

    /** 按大类聚合统计(左栏数量补充) */
    @GetMapping("/category-stats")
    public R<List<Map<String, Object>>> stats() { return R.ok(mapper.categoryStats()); }

    @PostMapping
    public R<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        String err = validateAndNormalize(body, null);
        if (err != null) return R.error(400, err);
        String id = "type-class-" + UUID.randomUUID().toString().substring(0, 8);
        body.put("id", id);
        applyCreateDefaults(body);
        mapper.insert(body);
        return R.ok(mapper.findById(id));
    }

    @PutMapping("/{id}")
    public R<?> update(@PathVariable String id, @RequestBody Map<String, Object> body) {
        Map<String, Object> exist = mapper.findById(id);
        if (exist == null) return R.error(404, "类型类不存在");
        // 系统保护:禁改核心字段(大类、名称前缀、参数类型)
        if (toInt(exist.get("system_protected")) == 1) {
            for (String core : List.of("category_code", "name_prefix", "param_type")) {
                if (body.containsKey(core) && !Objects.equals(str(body.get(core)), str(exist.get(core))))
                    return R.error(400, "系统保护类型类不可修改核心字段:" + core);
            }
        }
        // 合并后校验
        Map<String, Object> merged = new HashMap<>(exist);
        merged.putAll(body);
        merged.put("id", id);
        String e = validateAndNormalize(merged, id);
        if (e != null) return R.error(400, e);
        merged.put("updated_at", now());
        normalizeFlags(merged);
        merged.put("allow_apply_types", toJsonStr(merged.get("allow_apply_types")));
        merged.put("param_options_json", toJsonStr(merged.get("param_options_json")));
        merged.put("param_validator_json", toJsonStr(merged.get("param_validator_json")));
        merged.put("param_json", toJsonStr(merged.get("param_json")));
        merged.put("depend_on_meta_ids", toJsonStr(merged.getOrDefault("depend_on_meta_ids", "[]")));
        mapper.update(merged);
        return R.ok(mapper.findById(id));
    }

    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable String id) {
        Map<String, Object> exist = mapper.findById(id);
        if (exist == null) return R.ok();
        if (toInt(exist.get("system_protected")) == 1) return R.error(400, "系统保护类型类不可删除");
        bindMapper.deleteByMeta(id);
        mapper.delete(id);
        return R.ok();
    }

    /** 切换弃用 */
    @PostMapping("/{id}/deprecate")
    public R<?> deprecate(@PathVariable String id, @RequestBody Map<String, Object> body) {
        int deprecated = toInt(body.getOrDefault("deprecated", 1));
        String reason = body.get("reason") != null ? String.valueOf(body.get("reason")) : null;
        mapper.setDeprecated(id, deprecated, reason, now());
        return R.ok();
    }

    // —— 校验 + 归一化 ——
    private String validateAndNormalize(Map<String, Object> body, String excludeId) {
        String cat = str(body.get("category_code"));
        String namePrefix = str(body.get("name_prefix"));
        if (cat.isBlank()) return "种类(category_code)必填";
        if (namePrefix.isBlank()) return "名称(name_prefix)必填";
        if (str(body.get("name_cn_base")).isBlank()) return "中文名称(name_cn_base)必填";
        Map<String, Object> catRow = categoryMapper.findByCode(cat);
        if (catRow == null) return "大类不存在:" + cat;
        // 白名单约束:类型类 allow_apply_types ⊆ 大类 global_allow_apply_types
        List<String> global = jsonList(catRow.get("global_allow_apply_types"));
        List<String> allow = jsonList(body.get("allow_apply_types"));
        if (allow.isEmpty()) { allow = new ArrayList<>(global); body.put("allow_apply_types", allow); }
        for (String a : allow) if (!global.contains(a)) return "适用载体 [" + a + "] 超出大类允许范围 " + global;
        // 唯一性:同大类下 name_prefix 唯一
        if (mapper.countSameName(cat, namePrefix, excludeId == null ? "" : excludeId) > 0)
            return "同一大类下名称已存在:" + namePrefix;
        return null;
    }

    private void applyCreateDefaults(Map<String, Object> b) {
        b.putIfAbsent("source_type", "platform_built");
        b.putIfAbsent("param_type", "text");
        b.putIfAbsent("frontend_component", "text_input");
        b.putIfAbsent("current_version_no", 1);
        b.putIfAbsent("sort_weight", 999);
        b.putIfAbsent("name_cn_base", b.get("name_prefix"));
        normalizeFlags(b);
        b.put("allow_apply_types", toJsonStr(b.get("allow_apply_types")));
        b.put("param_options_json", toJsonStr(b.get("param_options_json")));
        b.put("param_validator_json", toJsonStr(b.get("param_validator_json")));
        b.put("param_json", toJsonStr(b.get("param_json")));
        b.put("depend_on_meta_ids", toJsonStr(b.getOrDefault("depend_on_meta_ids", "[]")));
        // 仅插入用到的可空键补 null,避免 MyBatis 取不到键报错
        for (String k : List.of("icon", "color", "name_template", "group_tag", "param_options_json",
                "param_validator_json", "param_desc", "demo_value", "description", "replacement_meta_id",
                "deprecated_reason", "support_version_min", "create_user", "update_user"))
            b.putIfAbsent(k, null);
    }

    private void normalizeFlags(Map<String, Object> b) {
        for (String k : List.of("allow_multi_bind", "is_array_value", "system_protected", "is_deprecated"))
            b.put(k, toInt(b.getOrDefault(k, 0)));
    }

    private static String str(Object o) { return o == null ? "" : String.valueOf(o); }
    private static int toInt(Object o) {
        if (o instanceof Number n) return n.intValue();
        if (o instanceof Boolean b) return b ? 1 : 0;
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return 0; }
    }
}
