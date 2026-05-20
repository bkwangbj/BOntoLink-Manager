package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.entity.BizCategory;
import com.beiktech.bontolink.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/category")
public class CategoryController {
    @Autowired private CategoryService service;

    @GetMapping("/tree")
    public R<List<Map<String, Object>>> tree() {
        return R.ok(service.tree());
    }

    @GetMapping("/{id}")
    public R<BizCategory> get(@PathVariable String id) {
        return R.ok(service.get(id));
    }

    @GetMapping("/{id}/stats")
    public R<Map<String, Object>> stats(@PathVariable String id) {
        return R.ok(service.stats(id));
    }

    @GetMapping("/{id}/graph")
    public R<Map<String, Object>> graph(@PathVariable String id) {
        return R.ok(service.graph(id));
    }

    /** 常用领域选择弹窗数据 */
    @GetMapping("/picker")
    public R<Map<String, Object>> picker() {
        return R.ok(service.picker());
    }

    /** 批量统计：?ids=a,b,c */
    @GetMapping("/stats-batch")
    public R<Map<String, Map<String, Object>>> statsBatch(@RequestParam("ids") String ids) {
        if (ids == null || ids.isEmpty()) return R.ok(java.util.Collections.emptyMap());
        java.util.List<String> list = java.util.Arrays.stream(ids.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
        return R.ok(service.statsBatch(list));
    }

    @PostMapping
    public R<BizCategory> create(@RequestBody BizCategory c) {
        return R.ok(service.create(c));
    }

    @PutMapping("/{id}")
    public R<BizCategory> update(@PathVariable String id, @RequestBody BizCategory c) {
        c.setId(id);
        return R.ok(service.update(c));
    }

    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable String id) {
        service.delete(id);
        return R.ok();
    }

    /** —— 分组成员管理（仅 categoryType=3 节点可用） —— */

    @Autowired private com.beiktech.bontolink.service.GroupService groupService;

    @PostMapping("/{id}/members")
    public R<?> addMember(@PathVariable String id, @RequestBody Map<String, String> body) {
        groupService.addMember(id, body.get("classId"));
        return R.ok();
    }

    @DeleteMapping("/{id}/members/{classId}")
    public R<?> removeMember(@PathVariable String id, @PathVariable String classId) {
        groupService.removeMember(id, classId);
        return R.ok();
    }

    @PutMapping("/{id}/members/reorder")
    public R<?> reorderMembers(@PathVariable String id, @RequestBody Map<String, List<String>> body) {
        groupService.reorderMembers(id, body.getOrDefault("classIds", List.of()));
        return R.ok();
    }
}
