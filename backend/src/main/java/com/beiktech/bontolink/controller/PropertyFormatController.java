package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.mapper.PropertyFormatMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 属性格式化配置 REST 接口：与属性 1:1 关联，upsert by property_id
 */
@RestController
@RequestMapping("/api/property-format")
public class PropertyFormatController {

    @Autowired private PropertyFormatMapper mapper;

    /** 批量查询：传入若干 propertyId,返回 property_id -> format 映射 */
    @GetMapping("/by-properties")
    public R<List<Map<String, Object>>> byProperties(@RequestParam("ids") List<String> ids) {
        if (ids == null || ids.isEmpty()) return R.ok(Collections.emptyList());
        return R.ok(mapper.findByProperties(ids));
    }

    @GetMapping("/property/{propertyId}")
    public R<Map<String, Object>> getOne(@PathVariable String propertyId) {
        Map<String, Object> r = mapper.findByProperty(propertyId);
        return R.ok(r == null ? new LinkedHashMap<>() : r);
    }

    /** Upsert：根据 property_id 存在与否决定 insert 或 update */
    @PutMapping("/property/{propertyId}")
    public R<?> upsert(@PathVariable String propertyId, @RequestBody Map<String, Object> body) {
        body.put("property_id", propertyId);
        body.putIfAbsent("property_scope", "class");
        body.putIfAbsent("format_enabled", 0);
        body.putIfAbsent("format_type", "general");
        body.putIfAbsent("decimal_places", 2);
        body.putIfAbsent("use_thousand_sep", 0);
        body.putIfAbsent("negative_mode", 3);
        body.putIfAbsent("currency_symbol", "¥");
        body.putIfAbsent("accounting_align", 1);
        body.putIfAbsent("date_pattern", "yyyy-MM-dd");
        body.putIfAbsent("time_pattern", "HH:mm:ss");
        body.putIfAbsent("locale", "zh-CN");
        body.putIfAbsent("fraction_type", "# ?/?");
        body.putIfAbsent("special_type", "zipcode");
        body.putIfAbsent("custom_format", "G/通用格式");
        body.putIfAbsent("text_force", 0);
        body.putIfAbsent("percent_auto_multiply", 1);

        Map<String, Object> existing = mapper.findByProperty(propertyId);
        if (existing == null) {
            body.put("format_id", "property-format-" + UUID.randomUUID());
            mapper.insert(body);
        } else {
            mapper.updateByProperty(body);
        }
        return R.ok();
    }

    @DeleteMapping("/property/{propertyId}")
    public R<?> remove(@PathVariable String propertyId) {
        mapper.deleteByProperty(propertyId);
        return R.ok();
    }

    /**
     * 批量 upsert: 对 ids 数组中每个 property_id 应用同一份 format 配置.
     * 请求体: { "ids": ["cp-1","cp-2"], "format": {...}, "property_scope": "class" }
     */
    @PutMapping("/batch")
    @SuppressWarnings("unchecked")
    public R<Map<String, Object>> batchUpsert(@RequestBody Map<String, Object> body) {
        List<String> ids = (List<String>) body.get("ids");
        Map<String, Object> fmt = (Map<String, Object>) body.get("format");
        String scope = (String) body.getOrDefault("property_scope", "class");
        if (ids == null || ids.isEmpty() || fmt == null) {
            return R.error(400, "ids / format 必填");
        }
        int created = 0, updated = 0;
        for (String pid : ids) {
            Map<String, Object> payload = new LinkedHashMap<>(fmt);
            payload.put("property_id", pid);
            payload.put("property_scope", scope);
            payload.putIfAbsent("format_enabled", 0);
            payload.putIfAbsent("format_type", "general");
            payload.putIfAbsent("decimal_places", 2);
            payload.putIfAbsent("use_thousand_sep", 0);
            payload.putIfAbsent("negative_mode", 3);
            payload.putIfAbsent("currency_symbol", "¥");
            payload.putIfAbsent("accounting_align", 1);
            payload.putIfAbsent("date_pattern", "yyyy-MM-dd");
            payload.putIfAbsent("time_pattern", "HH:mm:ss");
            payload.putIfAbsent("locale", "zh-CN");
            payload.putIfAbsent("fraction_type", "# ?/?");
            payload.putIfAbsent("special_type", "zipcode");
            payload.putIfAbsent("custom_format", "G/通用格式");
            payload.putIfAbsent("text_force", 0);
            payload.putIfAbsent("percent_auto_multiply", 1);

            Map<String, Object> existing = mapper.findByProperty(pid);
            if (existing == null) {
                payload.put("format_id", "property-format-" + UUID.randomUUID());
                mapper.insert(payload); created++;
            } else {
                mapper.updateByProperty(payload); updated++;
            }
        }
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("total", ids.size());
        resp.put("created", created);
        resp.put("updated", updated);
        return R.ok(resp);
    }
}
