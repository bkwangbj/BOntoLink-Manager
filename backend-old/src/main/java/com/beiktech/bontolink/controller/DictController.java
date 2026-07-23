package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.service.DictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 字典管理：字典定义 + 字典条目（树形）。路径: /api/dict */
@RestController
@RequestMapping("/api/dict")
public class DictController {

    @Autowired
    private DictService dictService;

    // ==================== 字典定义 ====================

    @GetMapping
    public R<List<Map<String, Object>>> listDefs() {
        return R.ok(dictService.listDefs());
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> getDef(@PathVariable String id) {
        return R.ok(dictService.getDef(id));
    }

    @PostMapping
    public R<Map<String, Object>> createDef(@RequestBody Map<String, Object> body) {
        return R.ok(dictService.createDef(body));
    }

    @PutMapping("/{id}")
    public R<Map<String, Object>> updateDef(@PathVariable String id, @RequestBody Map<String, Object> body) {
        return R.ok(dictService.updateDef(id, body));
    }

    @DeleteMapping("/{id}")
    public R<?> deleteDef(@PathVariable String id) {
        dictService.deleteDef(id);
        return R.ok();
    }

    // ==================== 字典条目 ====================

    @GetMapping("/{dictId}/items")
    public R<List<Map<String, Object>>> listItems(@PathVariable String dictId) {
        return R.ok(dictService.listItems(dictId));
    }

    /** 树形结构（递归挂载 children） */
    @GetMapping("/{dictId}/items/tree")
    public R<List<Map<String, Object>>> treeItems(@PathVariable String dictId) {
        return R.ok(dictService.treeItems(dictId));
    }

    @PostMapping("/items")
    public R<Map<String, Object>> createItem(@RequestBody Map<String, Object> body) {
        return R.ok(dictService.createItem(body));
    }

    @PutMapping("/items/{id}")
    public R<Map<String, Object>> updateItem(@PathVariable String id, @RequestBody Map<String, Object> body) {
        return R.ok(dictService.updateItem(id, body));
    }

    @DeleteMapping("/items/{id}")
    public R<?> deleteItem(@PathVariable String id) {
        dictService.deleteItem(id);
        return R.ok();
    }

    // ==================== 公开查询（长效缓存） ====================

    @GetMapping("/code/{code}")
    public R<List<Map<String, Object>>> getByCode(@PathVariable String code) {
        return R.ok(dictService.getItemsByCode(code));
    }

    /** 手动刷新全部缓存 */
    @PostMapping("/refresh-cache")
    public R<?> refreshCache() {
        dictService.evictDictCache();
        return R.ok();
    }

    /** 手动刷新指定字典的缓存 */
    @PostMapping("/refresh-cache/{code}")
    public R<?> refreshCacheByCode(@PathVariable String code) {
        dictService.evictDictCacheByCode(code);
        return R.ok();
    }
}
