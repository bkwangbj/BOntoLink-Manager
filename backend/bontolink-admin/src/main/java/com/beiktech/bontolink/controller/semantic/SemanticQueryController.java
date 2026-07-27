package com.beiktech.bontolink.controller.semantic;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.service.semantic.SemanticQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 语义查询 Controller
 * 支持关键词扩展、本体匹配、层级查询
 */
@Slf4j
@RestController
@RequestMapping("/api/semantic-search")
public class SemanticQueryController {

    @Autowired
    private SemanticQueryService semanticQueryService;

    /**
     * 关键词扩展查询
     * 输入用户自然语言，返回扩展后的关键词
     */
    @GetMapping("/expand")
    public R<Map<String, Object>> expandQuery(@RequestParam String query) {
        try {
            Map<String, Object> result = semanticQueryService.expandQuery(query);
            return R.ok(result);
        } catch (Exception e) {
            log.error("查询扩展失败", e);
            return R.error(500, "查询扩展失败: " + e.getMessage());
        }
    }

    /**
     * 智能本体匹配
     * 根据用户查询，匹配最相关的本体类
     */
    @GetMapping("/match")
    public R<List<Map<String, Object>>> matchOntology(@RequestParam String query,
                                                       @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Map<String, Object>> matches = semanticQueryService.matchOntology(query, limit);
            return R.ok(matches);
        } catch (Exception e) {
            log.error("本体匹配失败", e);
            return R.error(500, "本体匹配失败: " + e.getMessage());
        }
    }

    /**
     * 通过 RID 精确查询本体
     */
    @GetMapping("/by-rid")
    public R<Map<String, Object>> findByRid(@RequestParam String rid) {
        try {
            Map<String, Object> ontology = semanticQueryService.findByRid(rid);
            if (ontology == null) {
                return R.error(404, "未找到 RID: " + rid);
            }
            return R.ok(ontology);
        } catch (Exception e) {
            log.error("RID 查询失败", e);
            return R.error(500, "查询失败: " + e.getMessage());
        }
    }

    /**
     * 批量 RID 查询
     */
    @PostMapping("/batch-by-rid")
    public R<List<Map<String, Object>>> findBatchByRid(@RequestBody List<String> rids) {
        try {
            List<Map<String, Object>> results = semanticQueryService.findBatchByRid(rids);
            return R.ok(results);
        } catch (Exception e) {
            log.error("批量 RID 查询失败", e);
            return R.error(500, "查询失败: " + e.getMessage());
        }
    }

    /**
     * 查询父类列表
     */
    @GetMapping("/hierarchy/parents")
    public R<List<Map<String, Object>>> findParents(@RequestParam String classId) {
        try {
            List<Map<String, Object>> parents = semanticQueryService.findParents(classId);
            return R.ok(parents);
        } catch (Exception e) {
            log.error("查询父类失败", e);
            return R.error(500, "查询失败: " + e.getMessage());
        }
    }

    /**
     * 查询子类列表
     */
    @GetMapping("/hierarchy/children")
    public R<List<Map<String, Object>>> findChildren(@RequestParam String classId) {
        try {
            List<Map<String, Object>> children = semanticQueryService.findChildren(classId);
            return R.ok(children);
        } catch (Exception e) {
            log.error("查询子类失败", e);
            return R.error(500, "查询失败: " + e.getMessage());
        }
    }

    /**
     * 查询完整层级树（向上到根节点，向下到叶子节点）
     */
    @GetMapping("/hierarchy/tree")
    public R<Map<String, Object>> getHierarchyTree(@RequestParam String classId) {
        try {
            Map<String, Object> tree = semanticQueryService.getHierarchyTree(classId);
            return R.ok(tree);
        } catch (Exception e) {
            log.error("查询层级树失败", e);
            return R.error(500, "查询失败: " + e.getMessage());
        }
    }
}
