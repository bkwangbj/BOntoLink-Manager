package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.data.mapper.TypeClassCategoryMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** 类型类一级大类(Kind)字典接口。路径:/api/tc-category */
@RestController
@RequestMapping("/api/tc-category")
public class TypeClassCategoryController {

    @Autowired private TypeClassCategoryMapper mapper;
    private final ObjectMapper om = new ObjectMapper();
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private String now() { return LocalDateTime.now().format(TS); }
    private String toJson(Object v) {
        if (v == null) return "[]";
        if (v instanceof String s) return s.isBlank() ? "[]" : s;
        try { return om.writeValueAsString(v); } catch (Exception e) { return "[]"; }
    }

    @GetMapping
    public R<List<Map<String, Object>>> list() { return R.ok(mapper.listWithCount()); }

    @GetMapping("/{code}")
    public R<Map<String, Object>> get(@PathVariable String code) { return R.ok(mapper.findByCode(code)); }

    @PostMapping
    public R<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        String code = body.get("category_code") == null ? "" : String.valueOf(body.get("category_code")).trim();
        if (code.isBlank()) return R.error(400, "大类编码必填");
        if (body.get("category_name_cn") == null || String.valueOf(body.get("category_name_cn")).isBlank())
            return R.error(400, "大类中文名必填");
        if (mapper.findByCode(code) != null) return R.error(400, "大类编码已存在:" + code);
        body.put("category_code", code);
        body.put("global_allow_apply_types", toJson(body.get("global_allow_apply_types")));
        body.putIfAbsent("source_type", "user_custom");
        body.putIfAbsent("sort_weight", 999);
        for (String k : List.of("icon", "color", "description")) body.putIfAbsent(k, null);
        mapper.insert(body);
        return R.ok(mapper.findByCode(code));
    }

    @PutMapping("/{code}")
    public R<?> update(@PathVariable String code, @RequestBody Map<String, Object> body) {
        Map<String, Object> exist = mapper.findByCode(code);
        if (exist == null) return R.error(404, "大类不存在");
        Map<String, Object> merged = new HashMap<>(exist);
        merged.putAll(body);
        merged.put("category_code", code);
        merged.put("global_allow_apply_types", toJson(merged.get("global_allow_apply_types")));
        merged.put("updated_at", now());
        mapper.update(merged);
        return R.ok(mapper.findByCode(code));
    }

    @DeleteMapping("/{code}")
    public R<?> delete(@PathVariable String code) {
        int n = mapper.countTypeClasses(code);
        if (n > 0) return R.error(400, "该大类下仍有 " + n + " 个类型类,无法删除");
        mapper.delete(code);
        return R.ok();
    }
}
