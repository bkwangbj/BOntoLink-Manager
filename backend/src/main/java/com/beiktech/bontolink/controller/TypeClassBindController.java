package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.mapper.TypeClassBindMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** 类型类绑定实例接口。路径:/api/tc-bind */
@RestController
@RequestMapping("/api/tc-bind")
public class TypeClassBindController {

    @Autowired private TypeClassBindMapper mapper;
    private final ObjectMapper om = new ObjectMapper();
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private String now() { return LocalDateTime.now().format(TS); }
    private String toJsonStr(Object v) {
        if (v == null) return null;
        if (v instanceof String) return (String) v;
        try { return om.writeValueAsString(v); } catch (Exception e) { return null; }
    }

    /**
     * 按载体反查已绑定类型类(供对象属性/链接详情「已绑定类型类」区)。
     * property:传 ownerId(所属对象/接口 RID) + propertyId(属性 id);relation:传 linkTypeId。
     */
    @GetMapping("/by-carrier")
    public R<List<Map<String, Object>>> byCarrier(@RequestParam String applicableType,
                                                  @RequestParam(required = false) String ownerId,
                                                  @RequestParam(required = false) String propertyId,
                                                  @RequestParam(required = false) String linkTypeId) {
        if ("relation".equals(applicableType)) return R.ok(mapper.listByLink(linkTypeId));
        return R.ok(mapper.listByProperty(ownerId, propertyId));
    }

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
        body.put("value", toJsonStr(body.get("value")));   // 参数值对象 → JSON 字符串
        mapper.insert(body);
        return R.ok(mapper.findById(id));
    }

    /** 更新绑定的参数值(参数编辑弹窗保存) */
    @PutMapping("/{id}")
    public R<?> updateValue(@PathVariable String id, @RequestBody Map<String, Object> body) {
        if (mapper.findById(id) == null) return R.error(404, "绑定不存在");
        mapper.updateValue(id, toJsonStr(body.get("value")), now());
        return R.ok(mapper.findById(id));
    }

    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable String id) { mapper.delete(id); return R.ok(); }

    private static String s(Object o) { return o == null ? "" : String.valueOf(o); }
}
