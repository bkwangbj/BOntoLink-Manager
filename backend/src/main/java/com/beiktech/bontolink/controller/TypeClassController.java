package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.mapper.TypeClassMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 类型类 (Type Classes) REST 接口
 * <p>
 * 路径: /api/type-classes
 */
@RestController
@RequestMapping("/api/type-classes")
public class TypeClassController {

    @Autowired private TypeClassMapper mapper;

    /**
     * 列表 (支持按 applicable_type / category / is_deprecated 过滤)
     * @param applicableType property / relation / action
     * @param category       vertex / timeseries / hubble / ...
     * @param deprecated     0=只看可用, 1=只看已弃用, 不传=全部
     * @param catalogOnly    true=仅看目录预置 (未挂到具体对象)
     */
    @GetMapping
    public R<List<Map<String, Object>>> list(
            @RequestParam(required = false) String applicableType,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer deprecated,
            @RequestParam(required = false) Boolean catalogOnly) {
        return R.ok(mapper.list(applicableType, category, deprecated, catalogOnly));
    }

    /** 按 ID 查单个类型类 */
    @GetMapping("/{id}")
    public R<Map<String, Object>> get(@PathVariable String id) {
        return R.ok(mapper.findById(id));
    }

    /** 按种类聚合统计 (前端左树用) */
    @GetMapping("/category-stats")
    public R<List<Map<String, Object>>> stats() { return R.ok(mapper.categoryStats()); }

    /** 新建类型类；applicable_type/category/name 必填，ID 取 UUID 前 8 位 */
    @PostMapping
    public R<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        if (body.get("applicable_type") == null || String.valueOf(body.get("applicable_type")).isBlank())
            return R.error(400, "applicable_type 必填");
        if (body.get("category") == null || String.valueOf(body.get("category")).isBlank())
            return R.error(400, "category 必填");
        if (body.get("name") == null || String.valueOf(body.get("name")).isBlank())
            return R.error(400, "name 必填");

        // ID 只取 UUID 前 8 位，保持简短
        String id = "type-class-" + UUID.randomUUID().toString().substring(0, 8);
        body.put("id", id);
        body.putIfAbsent("is_deprecated", 0);
        body.putIfAbsent("name_cn", body.get("name"));
        body.putIfAbsent("category_cn", body.get("category"));
        mapper.insert(body);
        return R.ok(mapper.findById(id));
    }

    /** 更新类型类基本信息 */
    @PutMapping("/{id}")
    public R<?> update(@PathVariable String id, @RequestBody Map<String, Object> body) {
        body.put("id", id);
        body.putIfAbsent("is_deprecated", 0);
        mapper.update(body);
        return R.ok();
    }

    /** 删除类型类 */
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable String id) {
        mapper.delete(id);
        return R.ok();
    }

    /** 切换弃用 */
    @PostMapping("/{id}/deprecate")
    public R<?> deprecate(@PathVariable String id, @RequestBody Map<String, Object> body) {
        int deprecated = body.get("deprecated") instanceof Number
                ? ((Number) body.get("deprecated")).intValue() : 1;
        mapper.setDeprecated(id, deprecated);
        return R.ok();
    }
}
