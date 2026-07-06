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
    @Autowired private com.beiktech.bontolink.mapper.BizGroupMapper groupMapper;

    /** 查询所有值类型列表 */
    @GetMapping
    public R<List<Map<String, Object>>> list() { return R.ok(mapper.listAll()); }

    /** 按 ID 查单个值类型 */
    @GetMapping("/{id}")
    public R<Map<String, Object>> get(@PathVariable String id) {
        return R.ok(mapper.findById(id));
    }

    /** 新建值类型；ID 格式 "value-types-{uuid}"，rid 按 UUID 后段自动生成 */
    @PostMapping
    public R<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        String id = "value-types-" + UUID.randomUUID();
        body.put("id", id);
        body.putIfAbsent("rid", "ri.ont.value.types." + id.replaceFirst("^value-types-", ""));
        body.putIfAbsent("status", 1);
        mapper.insert(body);
        return R.ok(body);
    }

    /** 更新值类型基本信息 */
    @PutMapping("/{id}")
    public R<?> update(@PathVariable String id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        body.putIfAbsent("status", 1);
        mapper.update(body);
        return R.ok();
    }

    /** 删除值类型 */
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable String id) {
        groupMapper.deleteRefsByRefId(id, "value_types");
        mapper.delete(id);
        return R.ok();
    }

    /* 默认使用配置 */
    /** 查询所有值类型默认使用配置（ont_valuetypes_usage_config） */
    @GetMapping("/usage-configs")
    public R<List<Map<String, Object>>> listUsageConfigs() { return R.ok(mapper.listUsageConfigs()); }

    /** 新建使用配置；max_select_level/allow_non_leaf/display_format 不传时使用安全默认值 */
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

    /** 更新使用配置；同 create 补齐默认值，防止 NOT NULL 列写入 null */
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
