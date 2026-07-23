package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.data.mapper.LinkTypeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 链接类型 (Link Types) REST 接口
 * <p>
 * 路径: /api/link-types
 */
@RestController
@RequestMapping("/api/link-types")
public class LinkTypeController {

    @Autowired private LinkTypeMapper mapper;

    /** 查询所有链接类型列表 */
    @GetMapping
    public R<List<Map<String, Object>>> list() { return R.ok(mapper.listAll()); }

    /** 详情 (含 mappings + type_classes) */
    @GetMapping("/{id}")
    public R<Map<String, Object>> get(@PathVariable String id) {
        Map<String, Object> row = mapper.findById(id);
        if (row == null) return R.ok(null);
        row.put("mappings", mapper.listMappings(id));
        row.put("type_classes", mapper.listTypeClasses(id));
        return R.ok(row);
    }

    /** 查询指定链接类型的字段映射列表（ont_link_mappings） */
    @GetMapping("/{id}/mappings")
    public R<List<Map<String, Object>>> mappings(@PathVariable String id) {
        return R.ok(mapper.listMappings(id));
    }

    /** 创建 (可同时带 mappings) */
    @PostMapping
    @SuppressWarnings("unchecked")
    public R<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        String code = String.valueOf(body.getOrDefault("link_type_id", "")).trim();
        if (code.isEmpty()) return R.error(400, "链接类型 ID (link_type_id) 不能为空");
        if (!code.matches("^[a-z][a-z0-9-]*$"))
            return R.error(400, "link_type_id 只能含小写字母 / 数字 / 短划线, 且首字符必须为小写字母");
        if (mapper.existsByCode(code) != null) return R.error(400, "link_type_id " + code + " 已存在");

        String id = "link-types-" + UUID.randomUUID();
        body.put("id", id);
        // rid 自动生成: 前端会带 rid:"" 空串, putIfAbsent 不覆盖, 故按"空白则生成"处理
        String rid = String.valueOf(body.getOrDefault("rid", "")).trim();
        if (rid.isEmpty() || "null".equalsIgnoreCase(rid)) body.put("rid", "ri.ont.link.types." + code);
        body.putIfAbsent("status", "experimental");
        body.putIfAbsent("l_cardinality", "one");
        body.putIfAbsent("r_cardinality", "one");
        body.putIfAbsent("l_visibility", "normal");
        body.putIfAbsent("r_visibility", "normal");
        body.putIfAbsent("l_enabled", 1);
        body.putIfAbsent("r_enabled", 1);
        body.putIfAbsent("is_data_source_rel", 0);
        if (Integer.parseInt(String.valueOf(body.get("is_data_source_rel"))) == 0) {
            body.put("rel_data_table", null);
        }
        body.putIfAbsent("created_by", "admin");
        body.putIfAbsent("updated_by", "admin");
        mapper.insert(body);
        upsertMappings(id, body);
        return R.ok(mapper.findById(id));
    }

    /** 更新 (mappings 整体覆盖) */
    @PutMapping("/{id}")
    @SuppressWarnings("unchecked")
    public R<?> update(@PathVariable String id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        body.putIfAbsent("updated_by", "admin");
        // 历史数据 rid 为空时, 按 link_type_id 补生成 (重存即修复)
        String rid = String.valueOf(body.getOrDefault("rid", "")).trim();
        if (rid.isEmpty() || "null".equalsIgnoreCase(rid)) {
            String code = String.valueOf(body.getOrDefault("link_type_id", "")).trim();
            body.put("rid", code.isEmpty() ? null : "ri.ont.link.types." + code);
        }
        if (body.get("is_data_source_rel") instanceof Number
                && ((Number) body.get("is_data_source_rel")).intValue() == 0) {
            body.put("rel_data_table", null);
        }
        mapper.update(body);
        if (body.containsKey("mappings")) {
            mapper.deleteMappingsByLink(id);
            upsertMappings(id, body);
        }
        return R.ok();
    }

    @SuppressWarnings("unchecked")
    private void upsertMappings(String linkId, Map<String, Object> body) {
        Object raw = body.get("mappings");
        if (!(raw instanceof List<?>)) return;
        List<Map<String, Object>> mappings = (List<Map<String, Object>>) raw;
        for (Map<String, Object> m : mappings) {
            Object field = m.get("object_field");
            if (field == null || String.valueOf(field).isEmpty()) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("mapping_id", "link-mappings-" + UUID.randomUUID().toString().substring(0, 12));
            row.put("link_id", linkId);
            row.put("side", String.valueOf(m.getOrDefault("side", "left")));
            row.put("seq", m.getOrDefault("seq", 1));
            row.put("object_field", field);
            row.put("join_table_column", m.get("join_table_column"));
            mapper.insertMapping(row);
        }
    }

    /** 删除链接类型，级联删除其 mappings 和 type_class 关联 */
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable String id) {
        mapper.deleteMappingsByLink(id);
        mapper.deleteTypeClassesByLink(id);
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
            mapper.deleteMappingsByLink(id);
            mapper.deleteTypeClassesByLink(id);
            mapper.delete(id);
            ok++;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("deleted", ok);
        return R.ok(result);
    }

    /** 切换启用 / 实验 / 废弃 状态 */
    @PostMapping("/{id}/status")
    public R<?> setStatus(@PathVariable String id, @RequestBody Map<String, Object> body) {
        Map<String, Object> row = mapper.findById(id);
        if (row == null) return R.error(404, "未找到");
        row.put("status", body.getOrDefault("status", "experimental"));
        row.putIfAbsent("updated_by", "admin");
        mapper.update(row);
        return R.ok();
    }
}
