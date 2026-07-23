package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.data.mapper.ValueTypeMapper;
import com.beiktech.bontolink.data.mapper.EnumTypeMapper;
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
    @Autowired private com.beiktech.bontolink.data.mapper.BizGroupMapper groupMapper;
    @Autowired private EnumTypeMapper enumTypeMapper;

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

    /**
     * 同步枚举到值类型：为所有枚举类型创建对应的值类型（如果尚不存在）
     * 返回创建的数量和跳过的数量
     */
    @PostMapping("/sync-from-enums")
    public R<Map<String, Object>> syncFromEnums() {
        // 查询所有枚举类型
        List<Map<String, Object>> enumTypes = enumTypeMapper.listTypes();

        // 查询所有现有值类型（constraint_type = 'Enum'）
        List<Map<String, Object>> existingValueTypes = mapper.listAll();
        Set<String> existingEnumIds = new HashSet<>();
        for (Map<String, Object> vt : existingValueTypes) {
            if ("Enum".equals(vt.get("constraint_type")) || "Enum".equals(vt.get("constraintType"))) {
                String enumId = vt.get("enum_id") != null ? String.valueOf(vt.get("enum_id")) : String.valueOf(vt.get("enumId"));
                if (enumId != null && !"null".equals(enumId)) {
                    existingEnumIds.add(enumId);
                }
            }
        }

        int createdCount = 0;
        int skippedCount = 0;
        List<String> createdApiNames = new ArrayList<>();

        for (Map<String, Object> enumType : enumTypes) {
            String enumId = String.valueOf(enumType.get("id"));

            // 如果已存在对应的值类型，跳过
            if (existingEnumIds.contains(enumId)) {
                skippedCount++;
                continue;
            }

            // 创建值类型
            String valueTypeId = "value-types-" + UUID.randomUUID();
            Map<String, Object> valueType = new HashMap<>();
            valueType.put("id", valueTypeId);
            valueType.put("rid", "ri.ont.value.types." + valueTypeId.replaceFirst("^value-types-", ""));
            valueType.put("api_name", enumType.get("api_name") != null ? enumType.get("api_name") : enumType.get("apiName"));
            valueType.put("rdfs_label", enumType.get("rdfs_label") != null ? enumType.get("rdfs_label") : enumType.get("rdfsLabel"));
            valueType.put("category_code", enumType.get("category_code") != null ? enumType.get("category_code") : enumType.get("categoryCode"));
            valueType.put("base_type", "String");
            valueType.put("constraint_type", "Enum");
            valueType.put("enum_id", enumId);
            valueType.put("status", 1);

            try {
                mapper.insert(valueType);
                createdCount++;
                createdApiNames.add(String.valueOf(valueType.get("api_name")));
            } catch (Exception e) {
                // 可能是 api_name 重复等，记录但不中断
                System.err.println("创建值类型失败 (enumId=" + enumId + "): " + e.getMessage());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total_enums", enumTypes.size());
        result.put("created_count", createdCount);
        result.put("skipped_count", skippedCount);
        result.put("created_api_names", createdApiNames);

        return R.ok(result);
    }
}
