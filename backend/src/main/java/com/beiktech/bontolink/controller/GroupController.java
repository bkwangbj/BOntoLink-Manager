package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.entity.BizGroup;
import com.beiktech.bontolink.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/group")
public class GroupController {
    @Autowired private GroupService service;

    @GetMapping
    public R<List<BizGroup>> list(@RequestParam(value = "parentId", required = false) String parentId) {
        return R.ok(service.listByParent(parentId));
    }

    /** 返回所有分组 (扁平,前端组装树) */
    @GetMapping("/all")
    public R<List<BizGroup>> listAll() {
        return R.ok(service.listAll());
    }

    @PostMapping
    public R<BizGroup> create(@RequestBody BizGroup g) { return R.ok(service.create(g)); }

    @PutMapping("/{id}")
    public R<BizGroup> update(@PathVariable String id, @RequestBody BizGroup g) {
        g.setId(id);
        return R.ok(service.update(g));
    }

    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable String id) {
        service.delete(id);
        return R.ok();
    }

    @GetMapping("/{id}/classes")
    public R<List<Map<String, Object>>> classes(@PathVariable String id) {
        return R.ok(service.listClasses(id));
    }
}
