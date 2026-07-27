package com.beiktech.bontolink.ontology.controller;

import com.beiktech.bontolink.ontology.model.EntityMatch;
import com.beiktech.bontolink.ontology.model.ExpandedText;
import com.beiktech.bontolink.ontology.service.OntologyModelManager;
import com.beiktech.bontolink.ontology.service.expansion.SemanticExpansionService;
import com.beiktech.bontolink.ontology.service.matching.OntologyMatchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 本体匹配 Controller
 * <p>
 * 依赖 OntologyModelManager 管理 OntModel 生命周期：
 * - 查询时自动检查版本号，落后则异步重建
 * - 支持手动触发 rebuild
 */
@Slf4j
@RestController
@RequestMapping("/api/ontology")
public class OntologyMatchController {

    private final SemanticExpansionService expansionService;
    private final OntologyMatchService matchService;
    private final OntologyModelManager modelManager;

    public OntologyMatchController(SemanticExpansionService expansionService,
                                   OntologyMatchService matchService,
                                   OntologyModelManager modelManager) {
        this.expansionService = expansionService;
        this.matchService = matchService;
        this.modelManager = modelManager;
    }

    /**
     * 预览扩充效果
     */
    @PostMapping("/preview-expansion")
    public Map<String, Object> previewExpansion(@RequestBody Map<String, String> body) {
        try {
            String query = body.get("query");
            if (query == null || query.trim().isEmpty()) {
                return error(400, "查询文本不能为空");
            }

            ExpandedText expanded = expansionService.expandQuery(query);

            Map<String, Object> result = new HashMap<>();
            result.put("original", expanded.getOriginalText());
            result.put("expanded", expanded.getExpandedText());
            result.put("synonyms", expanded.getSynonymMap());
            result.put("stats", expanded.getStats());

            return success(result);
        } catch (Exception e) {
            log.error("预览扩充失败", e);
            return error(500, "预览失败: " + e.getMessage());
        }
    }

    /**
     * 本体匹配检索
     */
    @PostMapping("/match")
    public Map<String, Object> match(@RequestBody Map<String, Object> body) {
        try {
            String query = (String) body.get("query");
            if (query == null || query.trim().isEmpty()) {
                return error(400, "查询文本不能为空");
            }

            Integer topK = body.get("topK") != null ? ((Number) body.get("topK")).intValue() : 10;
            Double threshold = body.get("threshold") != null ? ((Number) body.get("threshold")).doubleValue() : 0.6;

            // 扩充查询
            ExpandedText expanded = expansionService.expandQuery(query);

            // 匹配实体
            List<EntityMatch> matches = matchService.match(query, topK, threshold);

            Map<String, Object> result = new HashMap<>();
            result.put("query", query);
            result.put("expandedQuery", expanded.getExpandedText());
            result.put("matches", matches);
            result.put("totalMatches", matches.size());

            return success(result);
        } catch (Exception e) {
            log.error("匹配失败", e);
            return error(500, "匹配失败: " + e.getMessage());
        }
    }

    /**
     * 健康检查（含 OntModel 状态）
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> info = new HashMap<>();
        info.put("status", "running");
        info.put("modelVersion", modelManager.getLoadedVersion());
        info.put("dbVersion", modelManager.getDbVersion());
        info.put("rebuildCount", modelManager.getRebuildCount());
        info.put("modelReady", modelManager.getLoadedVersion() >= 0);
        return success(info);
    }

    /**
     * 强制重建 OntModel（admin 端修改本体数据后调用）
     */
    @PostMapping("/rebuild")
    public Map<String, Object> rebuild() {
        int size = modelManager.rebuild();
        Map<String, Object> result = new HashMap<>();
        result.put("success", size >= 0);
        result.put("statementCount", Math.max(0, size));
        result.put("version", modelManager.getLoadedVersion());
        return success(result);
    }

    /**
     * 获取 OntModel 统计信息
     */
    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return success(modelManager.getStats());
    }

    private Map<String, Object> success(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("msg", "success");
        response.put("data", data);
        return response;
    }

    private Map<String, Object> error(int code, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("code", code);
        response.put("msg", message);
        response.put("data", null);
        return response;
    }
}
