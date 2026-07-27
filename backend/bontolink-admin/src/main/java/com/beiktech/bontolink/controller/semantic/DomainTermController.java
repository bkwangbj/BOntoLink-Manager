package com.beiktech.bontolink.controller.semantic;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.service.semantic.DomainTermService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 领域术语管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/domain-term")
public class DomainTermController {

    @Autowired
    private DomainTermService domainTermService;

    @GetMapping
    public R<List<Map<String, Object>>> list() {
        try {
            List<Map<String, Object>> list = domainTermService.listAll();
            return R.ok(list);
        } catch (Exception e) {
            log.error("查询领域术语失败", e);
            return R.error(500, "查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> findById(@PathVariable String id) {
        try {
            Map<String, Object> data = domainTermService.findById(id);
            if (data == null) {
                return R.error(404, "领域术语不存在");
            }
            return R.ok(data);
        } catch (Exception e) {
            log.error("查询失败", e);
            return R.error(500, "查询失败: " + e.getMessage());
        }
    }

    @PostMapping
    public R<Map<String, Object>> create(@RequestBody Map<String, Object> data) {
        try {
            if (data.get("standard_term") == null) {
                return R.error(400, "标准术语不能为空");
            }
            if (data.get("common_terms") == null) {
                return R.error(400, "通用说法不能为空");
            }
            if (data.get("domain") == null) {
                return R.error(400, "领域不能为空");
            }

            String id = domainTermService.create(data);
            return R.ok(domainTermService.findById(id));
        } catch (Exception e) {
            log.error("创建失败", e);
            return R.error(500, "创建失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public R<Map<String, Object>> update(@PathVariable String id, @RequestBody Map<String, Object> data) {
        try {
            domainTermService.update(id, data);
            return R.ok(domainTermService.findById(id));
        } catch (Exception e) {
            log.error("更新失败", e);
            return R.error(500, "更新失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        try {
            domainTermService.delete(id);
            return R.ok();
        } catch (Exception e) {
            log.error("删除失败", e);
            return R.error(500, "删除失败: " + e.getMessage());
        }
    }
}
