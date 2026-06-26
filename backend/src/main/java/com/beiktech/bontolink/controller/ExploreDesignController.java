package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.mapper.ExploreDesignMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 实例探索「看板/设计」持久化接口。
 * - 默认看板:每对象类型一条(name=''),由 maker「保存」写回, 进入看板时自动加载。
 * - 命名设计:name 非空, 由「另存为」创建, 在布局选择器列出。
 * config 列存整个设计对象 JSON;出入参均为对象(由 Jackson 序列化/反序列化)。
 */
@RestController
@RequestMapping("/api/explore-design")
public class ExploreDesignController {

    @Autowired private ExploreDesignMapper mapper;
    private final ObjectMapper om = new ObjectMapper();
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String now() { return LocalDateTime.now().format(TS); }

    /** 把行的 config(JSON 字符串)还原为对象返回给前端 */
    private Map<String, Object> unwrap(Map<String, Object> row) {
        if (row == null) return null;
        Object cfg = row.get("config");
        if (cfg instanceof String s && !s.isEmpty()) {
            try { row.put("config", om.readValue(s, Object.class)); } catch (Exception ignore) {}
        }
        return row;
    }

    private String toJson(Object cfg) {
        if (cfg == null) return null;
        if (cfg instanceof String s) return s;
        try { return om.writeValueAsString(cfg); } catch (Exception e) { return null; }
    }

    /** 某对象类型下的命名设计列表 */
    @GetMapping
    public R<List<Map<String, Object>>> list(@RequestParam String classId) {
        List<Map<String, Object>> rows = mapper.listNamedByClass(classId);
        rows.forEach(this::unwrap);
        return R.ok(rows);
    }

    /** 某对象类型的默认看板(无则返回 null) */
    @GetMapping("/default")
    public R<Map<String, Object>> getDefault(@RequestParam String classId) {
        return R.ok(unwrap(mapper.getDefault(classId)));
    }

    /** upsert 默认看板:body { classId, config, kind? } */
    @PutMapping("/default")
    public R<Map<String, Object>> saveDefault(@RequestBody Map<String, Object> body) {
        String classId = String.valueOf(body.get("classId"));
        String kind = body.get("kind") != null ? String.valueOf(body.get("kind")) : "query";
        String config = toJson(body.get("config"));
        Map<String, Object> exist = mapper.getDefault(classId);
        if (exist != null) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", exist.get("id"));
            row.put("name", "");
            row.put("kind", kind);
            row.put("config", config);
            row.put("updatedAt", now());
            mapper.update(row);
            return R.ok(unwrap(mapper.findById(String.valueOf(exist.get("id")))));
        }
        Map<String, Object> row = new HashMap<>();
        String id = "design-" + UUID.randomUUID();
        row.put("id", id);
        row.put("classId", classId);
        row.put("name", "");
        row.put("kind", kind);
        row.put("config", config);
        mapper.insert(row);
        return R.ok(unwrap(mapper.findById(id)));
    }

    /** 新建命名设计:body { classId, name, kind?, config } */
    @PostMapping
    public R<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        String classId = String.valueOf(body.get("classId"));
        String name = String.valueOf(body.getOrDefault("name", ""));
        if (name.isEmpty()) return R.error(400, "设计名称不能为空");
        Map<String, Object> row = new HashMap<>();
        String id = "design-" + UUID.randomUUID();
        row.put("id", id);
        row.put("classId", classId);
        row.put("name", name);
        row.put("kind", body.get("kind") != null ? String.valueOf(body.get("kind")) : "query");
        row.put("config", toJson(body.get("config")));
        mapper.insert(row);
        return R.ok(unwrap(mapper.findById(id)));
    }

    /** 更新命名设计:body { name?, kind?, config } */
    @PutMapping("/{id}")
    public R<Map<String, Object>> update(@PathVariable String id, @RequestBody Map<String, Object> body) {
        Map<String, Object> exist = mapper.findById(id);
        if (exist == null) return R.error(404, "设计不存在");
        Map<String, Object> row = new HashMap<>();
        row.put("id", id);
        row.put("name", body.get("name") != null ? String.valueOf(body.get("name")) : exist.get("name"));
        row.put("kind", body.get("kind") != null ? String.valueOf(body.get("kind")) : exist.get("kind"));
        row.put("config", body.containsKey("config") ? toJson(body.get("config")) : exist.get("config"));
        row.put("updatedAt", now());
        mapper.update(row);
        return R.ok(unwrap(mapper.findById(id)));
    }

    /** 删除命名设计（默认看板不可从此接口删除，前端不应调用） */
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable String id) {
        mapper.deleteById(id);
        return R.ok();
    }
}
