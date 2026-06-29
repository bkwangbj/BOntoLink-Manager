package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.mapper.TypeClassBindMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/** 类型类绑定实例接口。路径:/api/tc-bind */
@RestController
@RequestMapping("/api/tc-bind")
public class TypeClassBindController {

    @Autowired private TypeClassBindMapper mapper;

    /** 某类型类的绑定明细(可按载体类型/关键词过滤) */
    @GetMapping
    public R<List<Map<String, Object>>> list(@RequestParam String metaId,
                                             @RequestParam(required = false) String applicableType,
                                             @RequestParam(required = false) String q) {
        List<Map<String, Object>> all = mapper.listByMeta(metaId);
        String kw = q == null ? null : q.trim().toLowerCase();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> r : all) {
            if (applicableType != null && !applicableType.isBlank() && !applicableType.equals(r.get("applicable_type"))) continue;
            if (kw != null && !kw.isEmpty()) {
                String hay = (s(r.get("property_owner_id")) + " " + s(r.get("property_id")) + " "
                        + s(r.get("link_type_id")) + " " + s(r.get("action_type_id")) + " " + s(r.get("remark"))).toLowerCase();
                if (!hay.contains(kw)) continue;
            }
            out.add(r);
        }
        return R.ok(out);
    }

    /** 某类型类绑定统计(总/属性/关系/操作) */
    @GetMapping("/stats")
    public R<Map<String, Object>> stats(@RequestParam String metaId) {
        Map<String, Object> s = mapper.statsByMeta(metaId);
        if (s == null) s = new LinkedHashMap<>();
        s.putIfAbsent("total", 0);
        s.putIfAbsent("property_count", 0);
        s.putIfAbsent("relation_count", 0);
        s.putIfAbsent("action_count", 0);
        return R.ok(s);
    }

    @PostMapping
    public R<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        if (body.get("type_class_meta_id") == null) return R.error(400, "type_class_meta_id 必填");
        if (body.get("applicable_type") == null) return R.error(400, "applicable_type 必填");
        String id = "bind-" + UUID.randomUUID().toString().substring(0, 8);
        body.put("id", id);
        body.putIfAbsent("env", "prod");
        body.putIfAbsent("bind_deprecated", 0);
        for (String k : List.of("property_owner_type", "property_owner_id", "property_id",
                "link_type_id", "action_type_id", "suffix_custom", "value", "remark", "create_user", "update_user"))
            body.putIfAbsent(k, null);
        mapper.insert(body);
        return R.ok(mapper.findById(id));
    }

    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable String id) { mapper.delete(id); return R.ok(); }

    private static String s(Object o) { return o == null ? "" : String.valueOf(o); }
}
