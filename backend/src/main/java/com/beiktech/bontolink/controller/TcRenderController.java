package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.mapper.TypeClassBindMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 类型类「渲染解析」接口。路径:/api/tc-render
 * 把某载体(对象类型 / 链接)下的类型类绑定 + 参数值,一次性聚合成渲染友好结构,
 * 供图表(时序)、图谱(vertex)、事件、hubble 等渲染面读取(前端 tcResolver 再转渲染指令)。
 */
@RestController
@RequestMapping("/api/tc-render")
public class TcRenderController {

    @Autowired private TypeClassBindMapper bindMapper;
    private final ObjectMapper om = new ObjectMapper();

    /** 参数值 JSON 字符串 → 对象(解析失败回退原串) */
    private Object parseVal(Object v) {
        if (v == null) return null;
        if (!(v instanceof String s) || s.isBlank()) return v;
        try { return om.readValue(s, Object.class); } catch (Exception e) { return s; }
    }

    /**
     * 解析某载体的类型类绑定。
     * @param classId    对象类型 id(按属性聚合)
     * @param linkTypeId 链接 id(按关系聚合)
     */
    @GetMapping("/resolve")
    public R<Map<String, Object>> resolve(@RequestParam(required = false) String classId,
                                          @RequestParam(required = false) String linkTypeId) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("classId", classId);
        out.put("linkTypeId", linkTypeId);

        // —— 对象类型:按属性分组 ——
        if (classId != null && !classId.isBlank()) {
            List<Map<String, Object>> binds = bindMapper.listByOwnerClass(classId);
            // property_id -> { property_id, api_name, display_name, data_type, typeClasses:[...] }
            Map<String, Map<String, Object>> byProp = new LinkedHashMap<>();
            for (Map<String, Object> b : binds) {
                String pid = String.valueOf(b.get("property_id"));
                Map<String, Object> prop = byProp.computeIfAbsent(pid, k -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("property_id", b.get("property_id"));
                    m.put("api_name", b.get("prop_api_name"));
                    m.put("display_name", b.get("prop_display_name"));
                    m.put("data_type", b.get("prop_data_type"));
                    m.put("typeClasses", new ArrayList<Map<String, Object>>());
                    return m;
                });
                Map<String, Object> tc = new LinkedHashMap<>();
                tc.put("category_code", b.get("category_code"));
                tc.put("name_prefix", b.get("name_prefix"));
                tc.put("name_template", b.get("name_template"));
                tc.put("name_cn_base", b.get("name_cn_base"));
                tc.put("bind_deprecated", b.get("bind_deprecated"));
                tc.put("value", parseVal(b.get("value")));
                //noinspection unchecked
                ((List<Map<String, Object>>) prop.get("typeClasses")).add(tc);
            }
            out.put("properties", new ArrayList<>(byProp.values()));
        } else {
            out.put("properties", new ArrayList<>());
        }

        // —— 链接:扁平绑定 ——
        if (linkTypeId != null && !linkTypeId.isBlank()) {
            List<Map<String, Object>> links = new ArrayList<>();
            for (Map<String, Object> b : bindMapper.listByLink(linkTypeId)) {
                Map<String, Object> tc = new LinkedHashMap<>();
                tc.put("category_code", b.get("category_code"));
                tc.put("name_prefix", b.get("name_prefix"));
                tc.put("name_template", b.get("name_template"));
                tc.put("name_cn_base", b.get("name_cn_base"));
                tc.put("value", parseVal(b.get("value")));
                links.add(tc);
            }
            out.put("links", links);
        } else {
            out.put("links", new ArrayList<>());
        }
        return R.ok(out);
    }

    /**
     * 批量:某大类下全部对象类型的属性绑定,按 classId 分组返回,每个值形如 { properties:[...] }
     * (与单点 resolve 的 properties 结构一致,前端可对每个 classId 复用同一解析器)。
     * 图谱一次拉取 categoryCode=vertex,逐节点应用渲染指令。
     */
    @GetMapping("/category-map")
    public R<Map<String, Object>> categoryMap(@RequestParam String categoryCode) {
        List<Map<String, Object>> binds = bindMapper.listByCategory(categoryCode);
        // classId -> propId -> property{typeClasses[]}
        Map<String, Map<String, Map<String, Object>>> byClass = new LinkedHashMap<>();
        for (Map<String, Object> b : binds) {
            String cid = String.valueOf(b.get("property_owner_id"));
            String pid = String.valueOf(b.get("property_id"));
            Map<String, Map<String, Object>> propMap = byClass.computeIfAbsent(cid, k -> new LinkedHashMap<>());
            Map<String, Object> prop = propMap.computeIfAbsent(pid, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("property_id", b.get("property_id"));
                m.put("api_name", b.get("prop_api_name"));
                m.put("display_name", b.get("prop_display_name"));
                m.put("typeClasses", new ArrayList<Map<String, Object>>());
                return m;
            });
            Map<String, Object> tc = new LinkedHashMap<>();
            tc.put("category_code", b.get("category_code"));
            tc.put("name_prefix", b.get("name_prefix"));
            tc.put("name_template", b.get("name_template"));
            tc.put("name_cn_base", b.get("name_cn_base"));
            tc.put("value", parseVal(b.get("value")));
            //noinspection unchecked
            ((List<Map<String, Object>>) prop.get("typeClasses")).add(tc);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Map<String, Object>>> e : byClass.entrySet()) {
            Map<String, Object> one = new LinkedHashMap<>();
            one.put("properties", new ArrayList<>(e.getValue().values()));
            out.put(e.getKey(), one);
        }
        return R.ok(out);
    }
}
