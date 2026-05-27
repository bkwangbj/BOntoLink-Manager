package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.mapper.ValueTypeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 值类型 (Value types) REST 接口
 */
@RestController
@RequestMapping("/api/value-types")
public class ValueTypeController {

    @Autowired private ValueTypeMapper mapper;

    @GetMapping
    public R<List<Map<String, Object>>> list() { return R.ok(mapper.listAll()); }

    @GetMapping("/{id}")
    public R<Map<String, Object>> get(@PathVariable String id) {
        return R.ok(mapper.findById(id));
    }

    @PostMapping
    public R<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        String id = "value-types-" + UUID.randomUUID();
        body.put("id", id);
        body.putIfAbsent("rid", "ri.ont.value.types." + id.replaceFirst("^value-types-", ""));
        body.putIfAbsent("status", 1);
        mapper.insert(body);
        return R.ok(body);
    }

    @PutMapping("/{id}")
    public R<?> update(@PathVariable String id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        body.putIfAbsent("status", 1);
        mapper.update(body);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable String id) {
        mapper.delete(id);
        return R.ok();
    }

    /* 默认使用配置 */
    @GetMapping("/usage-configs")
    public R<List<Map<String, Object>>> listUsageConfigs() { return R.ok(mapper.listUsageConfigs()); }

    @PostMapping("/usage-configs")
    public R<Map<String, Object>> createUsageConfig(@RequestBody Map<String, Object> body) {
        String id = "vt-usage-config-" + UUID.randomUUID();
        body.put("id", id);
        body.putIfAbsent("max_select_level", 0);
        body.putIfAbsent("allow_non_leaf", 0);
        body.putIfAbsent("display_format", "label");
        body.putIfAbsent("is_system_default", 0);
        mapper.insertUsageConfig(body);
        return R.ok(body);
    }

    @PutMapping("/usage-configs/{id}")
    public R<?> updateUsageConfig(@PathVariable String id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        body.putIfAbsent("max_select_level", 0);
        body.putIfAbsent("allow_non_leaf", 0);
        body.putIfAbsent("display_format", "label");
        body.putIfAbsent("is_system_default", 0);
        mapper.updateUsageConfig(body);
        return R.ok();
    }
}
