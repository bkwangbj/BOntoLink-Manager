package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.mapper.SharedPropertyMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 共享属性 (Shared Properties) REST 接口
 * <p>
 * 路径: /api/shared-properties
 */
@RestController
@RequestMapping("/api/shared-properties")
public class SharedPropertyController {

    @Autowired private SharedPropertyMapper mapper;

    /** 查询所有共享属性列表（含 ref_count 引用次数） */
    @GetMapping
    public R<List<Map<String, Object>>> list() { return R.ok(mapper.listAll()); }

    /** 按 ID 查单个共享属性 */
    @GetMapping("/{id}")
    public R<Map<String, Object>> get(@PathVariable String id) {
        return R.ok(mapper.findById(id));
    }

    /** 创建共享属性 */
    @PostMapping
    public R<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        String code = String.valueOf(body.getOrDefault("prop_code", "")).trim();
        if (code.isEmpty()) {
            return R.error(400,"属性编码不能为空");
        }
        if (mapper.existsByCode(code) != null) {
            return R.error(400,"属性编码 " + code + " 已存在");
        }
        String id = "shared-properties-" + UUID.randomUUID();
        body.put("id", id);
        body.putIfAbsent("rid", "ri.ont.shared.props." + code);
        body.putIfAbsent("prop_type", "data");
        body.putIfAbsent("is_key", 0);
        body.putIfAbsent("is_required", 0);
        body.putIfAbsent("is_multi_valued_prop", 0);
        body.putIfAbsent("is_range_constraint_prop", 0);
        body.putIfAbsent("owl_functional", 0);
        body.putIfAbsent("owl_inverse_functional", 0);
        body.putIfAbsent("owl_transitive", 0);
        body.putIfAbsent("owl_symmetric", 0);
        body.putIfAbsent("owl_asymmetric", 0);
        body.putIfAbsent("owl_reflexive", 0);
        body.putIfAbsent("owl_irreflexive", 0);
        body.putIfAbsent("status", 1);

        mapper.insert(body);

        // 可选: 创建时挂分组 (body.group_id)
        Object groupId = body.get("group_id");
        if (groupId != null && !String.valueOf(groupId).isEmpty()) {
            String gcId = "sp-gc-" + UUID.randomUUID().toString().substring(0, 12);
            mapper.insertGroupRef(gcId, String.valueOf(groupId), id,
                    String.valueOf(body.getOrDefault("category_code", "")), 0);
        }

        return R.ok(mapper.findById(id));
    }

    /** 更新共享属性 (prop_code 不可变, 由 mapper SQL 不更新该列保证) */
    @PutMapping("/{id}")
    public R<?> update(@PathVariable String id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        // 兜底默认值, 避免 null 写入 NOT NULL 列
        body.putIfAbsent("is_key", 0);
        body.putIfAbsent("is_required", 0);
        body.putIfAbsent("is_multi_valued_prop", 0);
        body.putIfAbsent("is_range_constraint_prop", 0);
        body.putIfAbsent("owl_functional", 0);
        body.putIfAbsent("owl_inverse_functional", 0);
        body.putIfAbsent("owl_transitive", 0);
        body.putIfAbsent("owl_symmetric", 0);
        body.putIfAbsent("owl_asymmetric", 0);
        body.putIfAbsent("owl_reflexive", 0);
        body.putIfAbsent("owl_irreflexive", 0);
        body.putIfAbsent("status", 1);
        body.putIfAbsent("prop_type", "data");
        mapper.update(body);
        return R.ok();
    }

    /** 删除共享属性 (调用方应在前端校验 ref_count=0) */
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable String id) {
        // 引用次数大于 0 拒绝删除
        Map<String, Object> row = mapper.findById(id);
        if (row == null) return R.ok();
        Object cnt = row.get("ref_count");
        long refCount = cnt instanceof Number ? ((Number) cnt).longValue() : 0L;
        if (refCount > 0) {
            return R.error(400,"该共享属性被引用 " + refCount + " 次, 无法删除");
        }
        mapper.deleteGroupRefs(id);
        mapper.delete(id);
        return R.ok();
    }

    /** 批量删除 */
    @PostMapping("/batch-delete")
    public R<Map<String, Object>> batchDelete(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> ids = (List<String>) body.getOrDefault("ids", Collections.emptyList());
        int ok = 0; int blocked = 0;
        List<String> blockedList = new ArrayList<>();
        for (String id : ids) {
            Map<String, Object> row = mapper.findById(id);
            if (row == null) continue;
            Object cnt = row.get("ref_count");
            long refCount = cnt instanceof Number ? ((Number) cnt).longValue() : 0L;
            if (refCount > 0) {
                blocked++;
                blockedList.add(String.valueOf(row.getOrDefault("rdfs_label", row.get("prop_code"))));
                continue;
            }
            mapper.deleteGroupRefs(id);
            mapper.delete(id);
            ok++;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("deleted", ok);
        result.put("blocked", blocked);
        result.put("blockedList", blockedList);
        return R.ok(result);
    }

    /** 引用列表: 哪些对象类属性引用了该共享属性 */
    @GetMapping("/{id}/references")
    public R<List<Map<String, Object>>> references(@PathVariable String id) {
        Map<String, Object> sp = mapper.findById(id);
        if (sp == null) return R.ok(Collections.emptyList());
        String code = String.valueOf(sp.getOrDefault("prop_code", ""));
        if (code.isEmpty()) return R.ok(Collections.emptyList());
        return R.ok(mapper.listReferences(code));
    }
}
