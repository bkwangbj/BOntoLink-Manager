package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.entity.BizGroup;
import com.beiktech.bontolink.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 业务分组管理接口 — 维护 ont_biz_group 树状分组，供对象类型、链接类型等模块挂载资源。
 */
@RestController
@RequestMapping("/api/group")
public class GroupController {
    @Autowired private GroupService service;

    /** 按父节点查询直接子分组；parentId 为空时返回根分组 */
    @GetMapping
    public R<List<BizGroup>> list(@RequestParam(value = "parentId", required = false) String parentId) {
        return R.ok(service.listByParent(parentId));
    }

    /** 返回所有分组 (扁平,前端组装树) */
    @GetMapping("/all")
    public R<List<BizGroup>> listAll() {
        return R.ok(service.listAll());
    }

    /** 新建分组节点，返回带 id 的完整分组对象 */
    @PostMapping
    public R<BizGroup> create(@RequestBody BizGroup g) { return R.ok(service.create(g)); }

    /** 更新分组名称/排序/父节点等字段 */
    @PutMapping("/{id}")
    public R<BizGroup> update(@PathVariable String id, @RequestBody BizGroup g) {
        g.setId(id);
        return R.ok(service.update(g));
    }

    /** 删除分组（含级联清理子分组及 group_ref 绑定） */
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable String id) {
        service.delete(id);
        return R.ok();
    }

    /** 查询该分组下绑定的所有对象类型（ont_biz_group_class 关联表） */
    @GetMapping("/{id}/classes")
    public R<List<Map<String, Object>>> classes(@PathVariable String id) {
        return R.ok(service.listClasses(id));
    }
}
