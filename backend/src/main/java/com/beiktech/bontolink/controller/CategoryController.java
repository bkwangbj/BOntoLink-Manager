package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.entity.BizCategory;
import com.beiktech.bontolink.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 行业分类（BizCategory）管理：分类树、统计、图谱及分组成员 CRUD */
@RestController
@RequestMapping("/api/category")
public class CategoryController {
    @Autowired private CategoryService service;

    /** 返回完整的行业分类树结构（含层级嵌套） */
    @GetMapping("/tree")
    public R<List<Map<String, Object>>> tree() {
        return R.ok(service.tree());
    }

    /** 按 id 获取单个分类节点详情 */
    @GetMapping("/{id}")
    public R<BizCategory> get(@PathVariable String id) {
        return R.ok(service.get(id));
    }

    /** 获取指定分类节点下的资源统计（对象类/链接等数量） */
    @GetMapping("/{id}/stats")
    public R<Map<String, Object>> stats(@PathVariable String id) {
        return R.ok(service.stats(id));
    }

    /** 获取指定分类的关联图谱数据（节点 + 边） */
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
        // 逗号分隔的 id 串拆分后过滤空串，再批量查统计
        java.util.List<String> list = java.util.Arrays.stream(ids.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
        return R.ok(service.statsBatch(list));
    }

    /** 新建行业分类节点 */
    @PostMapping
    public R<BizCategory> create(@RequestBody BizCategory c) {
        return R.ok(service.create(c));
    }

    /** 更新分类节点信息；路径 id 回写到实体后再更新 */
    @PutMapping("/{id}")
    public R<BizCategory> update(@PathVariable String id, @RequestBody BizCategory c) {
        c.setId(id);
        return R.ok(service.update(c));
    }

    /** 删除分类节点（service 层负责级联校验/子节点处理） */
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable String id) {
        service.delete(id);
        return R.ok();
    }

    /** —— 分组成员管理（仅 categoryType=3 节点可用） —— */

    @Autowired private com.beiktech.bontolink.service.GroupService groupService;

    /** 向分组节点添加对象类成员；body 含 classId */
    @PostMapping("/{id}/members")
    public R<?> addMember(@PathVariable String id, @RequestBody Map<String, String> body) {
        groupService.addMember(id, body.get("classId"));
        return R.ok();
    }

    /** 从分组节点移除指定对象类成员 */
    @DeleteMapping("/{id}/members/{classId}")
    public R<?> removeMember(@PathVariable String id, @PathVariable String classId) {
        groupService.removeMember(id, classId);
        return R.ok();
    }

    /** 对分组成员重新排序；body 含 classIds（有序 id 列表） */
    @PutMapping("/{id}/members/reorder")
    public R<?> reorderMembers(@PathVariable String id, @RequestBody Map<String, List<String>> body) {
        groupService.reorderMembers(id, body.getOrDefault("classIds", List.of()));
        return R.ok();
    }
}
