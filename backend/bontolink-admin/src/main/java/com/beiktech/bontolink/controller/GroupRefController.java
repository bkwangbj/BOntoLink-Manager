package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.data.mapper.BizGroupMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 统一分组关联表 API (ont_biz_group_class 的 ref_id + group_type 形态).
 *
 * group_type 取值:
 *   object_types / link_types / action_types / value_types / shared_props
 *   / functions / interface / datasources / enum_types
 */
@RestController
@RequestMapping("/api/group-refs")
public class GroupRefController {

    @Autowired private BizGroupMapper mapper;

    /** 列出某类型下所有的分组绑定 */
    @GetMapping
    public R<List<Map<String, Object>>> list(@RequestParam("type") String type) {
        return R.ok(mapper.listRefsByType(type));
    }

    /** 列出某分组下、某类型的全部资源绑定 */
    @GetMapping("/by-group")
    public R<List<Map<String, Object>>> byGroup(@RequestParam("groupId") String groupId,
                                                 @RequestParam("type") String type) {
        return R.ok(mapper.listRefsByGroupAndType(groupId, type));
    }

    /** 新建分组绑定 (idempotent: 若 ref_id+type 已存在则跳过) */
    @PostMapping
    public R<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        String type = (String) body.get("group_type");
        if (type == null || type.isBlank()) return R.error(400, "group_type 必填");
        if (body.get("ref_id") == null)     return R.error(400, "ref_id 必填");
        body.putIfAbsent("id", "group-ref-" + UUID.randomUUID());
        body.putIfAbsent("g_sort", 0);
        body.putIfAbsent("category_code", null);
        mapper.insertRef(body);
        return R.ok(body);
    }

    /** 更新分组绑定 (改 group_id / category_code / g_sort) */
    @PutMapping("/{id}")
    public R<?> update(@PathVariable String id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        body.putIfAbsent("g_sort", 0);
        mapper.updateRef(body);
        return R.ok();
    }

    /** 删除单条 */
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable String id) {
        mapper.deleteRefById(id);
        return R.ok();
    }

    /** 按 ref_id + type 删除 (用于资源删除时清理 group_class 记录) */
    @DeleteMapping("/by-ref")
    public R<?> deleteByRef(@RequestParam("refId") String refId, @RequestParam("type") String type) {
        mapper.deleteRefsByRefId(refId, type);
        return R.ok();
    }
}
