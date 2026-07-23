package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.service.PhysicalTableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 物理表/视图元数据接口。
 * 数据落库于 ont_physical_table; 同步源为后端自身库(对象类型新建向导"选择主表"用)。
 */
@RestController
@RequestMapping("/api/physical-tables")
public class PhysicalTableController {

    @Autowired private PhysicalTableService service;

    /** 列出已落库的物理表(dsId 为空则全部) */
    @GetMapping
    public R<List<Map<String, Object>>> list(@RequestParam(required = false) String dsId) {
        return R.ok(service.list(dsId));
    }

    /** 同步指定数据源的物理表(读自身库, 保留已有中文名) */
    @PostMapping("/sync")
    public R<List<Map<String, Object>>> sync(@RequestParam String dsId) {
        try {
            return R.ok(service.sync(dsId));
        } catch (Exception e) {
            return R.error(500, e.getMessage());
        }
    }

    /** 修改物理表中文名 */
    @PutMapping("/{id}/name")
    public R<Map<String, Object>> updateName(@PathVariable String id, @RequestBody Map<String, Object> body) {
        String name = body.get("displayName") != null ? String.valueOf(body.get("displayName"))
                : String.valueOf(body.getOrDefault("display_name", ""));
        return R.ok(service.updateName(id, name));
    }

    /** 删除物理表 */
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable String id) {
        service.delete(id);
        return R.ok();
    }
}
