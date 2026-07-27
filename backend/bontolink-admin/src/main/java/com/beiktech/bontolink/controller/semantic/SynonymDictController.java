package com.beiktech.bontolink.controller.semantic;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.service.semantic.SynonymDictService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 同义词词典管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/synonym-dict")
public class SynonymDictController {

    @Autowired
    private SynonymDictService synonymDictService;

    /**
     * 查询列表
     */
    @GetMapping
    public R<List<Map<String, Object>>> list(@RequestParam(required = false) String domain) {
        try {
            List<Map<String, Object>> list = synonymDictService.list(domain);
            return R.ok(list);
        } catch (Exception e) {
            log.error("查询同义词列表失败", e);
            return R.error(500, "查询失败: " + e.getMessage());
        }
    }

    /**
     * 根据 ID 查询
     */
    @GetMapping("/{id}")
    public R<Map<String, Object>> findById(@PathVariable String id) {
        try {
            Map<String, Object> data = synonymDictService.findById(id);
            if (data == null) {
                return R.error(404, "同义词不存在");
            }
            return R.ok(data);
        } catch (Exception e) {
            log.error("查询同义词失败", e);
            return R.error(500, "查询失败: " + e.getMessage());
        }
    }

    /**
     * 创建
     */
    @PostMapping
    public R<Map<String, Object>> create(@RequestBody Map<String, Object> data) {
        try {
            // 参数校验
            if (data.get("word") == null || data.get("word").toString().trim().isEmpty()) {
                return R.error(400, "主词不能为空");
            }
            if (data.get("synonyms") == null) {
                return R.error(400, "同义词列表不能为空");
            }

            String id = synonymDictService.create(data);
            Map<String, Object> created = synonymDictService.findById(id);
            return R.ok(created);
        } catch (Exception e) {
            log.error("创建同义词失败", e);
            return R.error(500, "创建失败: " + e.getMessage());
        }
    }

    /**
     * 更新
     */
    @PutMapping("/{id}")
    public R<Map<String, Object>> update(@PathVariable String id, @RequestBody Map<String, Object> data) {
        try {
            synonymDictService.update(id, data);
            Map<String, Object> updated = synonymDictService.findById(id);
            return R.ok(updated);
        } catch (Exception e) {
            log.error("更新同义词失败", e);
            return R.error(500, "更新失败: " + e.getMessage());
        }
    }

    /**
     * 删除
     */
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        try {
            synonymDictService.delete(id);
            return R.ok();
        } catch (Exception e) {
            log.error("删除同义词失败", e);
            return R.error(500, "删除失败: " + e.getMessage());
        }
    }

    /**
     * 批量导入
     */
    @PostMapping("/batch-import")
    public R<Map<String, Object>> batchImport(@RequestBody List<Map<String, Object>> dataList) {
        try {
            int count = synonymDictService.batchImport(dataList);
            return R.ok(Map.of(
                "total", dataList.size(),
                "success", count,
                "failed", dataList.size() - count
            ));
        } catch (Exception e) {
            log.error("批量导入失败", e);
            return R.error(500, "导入失败: " + e.getMessage());
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public R<String> health() {
        return R.ok("Synonym Dict API is running");
    }
}
