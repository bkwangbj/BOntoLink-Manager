package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.mapper.StructTypeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 结构属性 (Struct Types) REST 接口
 * <p>
 * 路径: /api/struct-types
 */
@RestController
@RequestMapping("/api/struct-types")
public class StructTypeController {

    @Autowired private StructTypeMapper mapper;

    @GetMapping
    public R<List<Map<String, Object>>> list() { return R.ok(mapper.listAll()); }

    @GetMapping("/{id}")
    public R<Map<String, Object>> get(@PathVariable String id) {
        Map<String, Object> row = mapper.findById(id);
        if (row != null) row.put("items", mapper.listItems(id));
        return R.ok(row);
    }

    @GetMapping("/{id}/items")
    public R<List<Map<String, Object>>> items(@PathVariable String id) {
        return R.ok(mapper.listItems(id));
    }

    /** 创建结构类型 (可同时带 items) */
    @PostMapping
    @SuppressWarnings("unchecked")
    public R<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        String code = String.valueOf(body.getOrDefault("struct_code", "")).trim();
        if (code.isEmpty()) return R.error(400, "结构编码不能为空");
        if (mapper.existsByCode(code) != null) return R.error(400, "结构编码 " + code + " 已存在");

        String id = "struct-types-" + UUID.randomUUID();
        body.put("id", id);
        body.putIfAbsent("status", 1);
        mapper.insert(body);

        // 可选: 同时插入 items
        Object rawItems = body.get("items");
        if (rawItems instanceof List<?>) {
            List<Map<String, Object>> items = (List<Map<String, Object>>) rawItems;
            int sort = 1;
            for (Map<String, Object> it : items) {
                Object propId = it.get("prop_id");
                if (propId == null) continue;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", "struct-items-" + UUID.randomUUID().toString().substring(0, 13));
                row.put("struct_id", id);
                row.put("sort_no", it.getOrDefault("sort_no", sort++));
                row.put("prop_id", propId);
                mapper.insertItem(row);
            }
        }
        Map<String, Object> created = mapper.findById(id);
        if (created != null) created.put("items", mapper.listItems(id));
        return R.ok(created);
    }

    /** 更新结构类型 (items 整体覆盖: 删除旧的 → 插入新的) */
    @PutMapping("/{id}")
    @SuppressWarnings("unchecked")
    public R<?> update(@PathVariable String id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        body.putIfAbsent("status", 1);
        mapper.update(body);

        // items 整体覆盖
        if (body.containsKey("items")) {
            mapper.deleteItemsByStruct(id);
            Object rawItems = body.get("items");
            if (rawItems instanceof List<?>) {
                List<Map<String, Object>> items = (List<Map<String, Object>>) rawItems;
                int sort = 1;
                for (Map<String, Object> it : items) {
                    Object propId = it.get("prop_id");
                    if (propId == null) continue;
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", "struct-items-" + UUID.randomUUID().toString().substring(0, 13));
                    row.put("struct_id", id);
                    row.put("sort_no", it.getOrDefault("sort_no", sort++));
                    row.put("prop_id", propId);
                    mapper.insertItem(row);
                }
            }
        }
        return R.ok();
    }

    /** 删除结构类型 (级联删除 items) */
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable String id) {
        mapper.deleteItemsByStruct(id);
        mapper.delete(id);
        return R.ok();
    }

    /** 批量删除 */
    @PostMapping("/batch-delete")
    @SuppressWarnings("unchecked")
    public R<Map<String, Object>> batchDelete(@RequestBody Map<String, Object> body) {
        List<String> ids = (List<String>) body.getOrDefault("ids", Collections.emptyList());
        int ok = 0;
        for (String id : ids) {
            mapper.deleteItemsByStruct(id);
            mapper.delete(id);
            ok++;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("deleted", ok);
        return R.ok(result);
    }
}
