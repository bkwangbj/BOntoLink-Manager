package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.tool.vector.VectorInitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 向量管理接口 — 用于 Milvus 向量库初始化和维护
 *
 * 仅在 bontolink.ontology.vector.type=milvus 时启用
 */
@Slf4j
@RestController
@RequestMapping("/api/vector-admin")
@ConditionalOnBean(VectorInitService.class)
public class VectorAdminController {

    private final VectorInitService vectorInitService;

    public VectorAdminController(VectorInitService vectorInitService) {
        this.vectorInitService = vectorInitService;
    }

    /**
     * POST /api/vector-admin/sync
     * 全量同步：从 ont_class 读取所有类并生成向量
     */
    @PostMapping("/sync")
    public R<Map<String, Object>> syncAll() {
        log.info("收到向量全量同步请求");
        Map<String, Object> result = vectorInitService.syncAll();
        if (Boolean.TRUE.equals(result.get("success"))) {
            return R.ok(result);
        }
        return R.error(500, String.valueOf(result.get("message")));
    }

    /**
     * POST /api/vector-admin/rebuild
     * 重建集合：删除旧集合，重新创建并同步数据
     */
    @PostMapping("/rebuild")
    public R<Map<String, Object>> rebuild() {
        log.info("收到向量重建请求");
        Map<String, Object> result = vectorInitService.rebuild();
        if (Boolean.TRUE.equals(result.get("success"))) {
            return R.ok(result);
        }
        return R.error(500, String.valueOf(result.get("message")));
    }

    /**
     * GET /api/vector-admin/status
     * 查看向量库状态：集合信息、记录数等
     */
    @GetMapping("/status")
    public R<Map<String, Object>> status() {
        Map<String, Object> result = vectorInitService.status();
        if (Boolean.TRUE.equals(result.get("success"))) {
            return R.ok(result);
        }
        return R.error(500, String.valueOf(result.get("message")));
    }

    /**
     * POST /api/vector-admin/sync-synonyms
     * 同步同义词：从 sys_synonym_dict 读取所有同义词并生成向量
     */
    @PostMapping("/sync-synonyms")
    public R<Map<String, Object>> syncSynonyms() {
        log.info("收到同义词向量同步请求");
        Map<String, Object> result = vectorInitService.syncSynonyms();
        if (Boolean.TRUE.equals(result.get("success"))) {
            return R.ok(result);
        }
        return R.error(500, String.valueOf(result.get("message")));
    }
}
