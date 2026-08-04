package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.service.FnRepoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 函数代码仓 REST 接口 (P5)
 * <p>
 * 路径: /api/fn-repo 。供函数在线编排 IDE 的项目文件树、编辑器读写、版本变更面板使用。
 * 仓库地址与凭据由环境变量注入, 本接口不接收也不回显任何凭据。
 */
@RestController
@RequestMapping("/api/fn-repo")
public class FnRepoController {

    @Autowired private FnRepoService repo;

    /** 仓库状态:是否就绪、当前分支、HEAD、是否有未提交改动 */
    @GetMapping("/status")
    public R<Map<String, Object>> status() { return R.ok(repo.status()); }

    /** 工作区文件树 (嵌套节点, 已跳过 .git) */
    @GetMapping("/tree")
    public R<List<Map<String, Object>>> tree() {
        try {
            return R.ok(repo.tree());
        } catch (Exception e) {
            return R.error(500, "读取文件树失败: " + e.getMessage());
        }
    }

    /** 读取单个文件内容 */
    @GetMapping("/file")
    public R<Map<String, Object>> read(@RequestParam String path) {
        try {
            return R.ok(Map.of("path", path, "content", repo.read(path)));
        } catch (Exception e) {
            return R.error(400, e.getMessage());
        }
    }

    /** 保存文件 → 提交 (auto-push 打开时顺带推送) */
    @PutMapping("/file")
    public R<Map<String, Object>> write(@RequestBody Map<String, Object> body) {
        String path = String.valueOf(body.getOrDefault("path", "")).trim();
        if (path.isEmpty()) return R.error(400, "path 不能为空");
        String content = body.get("content") == null ? "" : String.valueOf(body.get("content"));
        String message = body.get("message") == null ? null : String.valueOf(body.get("message"));
        try {
            return R.ok(repo.write(path, content, message));
        } catch (Exception e) {
            return R.error(500, "保存失败: " + e.getMessage());
        }
    }

    /** 提交历史;带 path 时只看该文件 */
    @GetMapping("/history")
    public R<List<Map<String, Object>>> history(@RequestParam(required = false) String path,
                                                @RequestParam(required = false, defaultValue = "30") int limit) {
        try {
            return R.ok(repo.history(path, limit));
        } catch (Exception e) {
            return R.error(500, "读取历史失败: " + e.getMessage());
        }
    }

    /** 手动推送 (auto-push 关闭时用) */
    @PostMapping("/push")
    public R<Map<String, Object>> push() {
        try {
            return R.ok(repo.pushNow());
        } catch (Exception e) {
            return R.error(500, "推送失败: " + e.getMessage());
        }
    }

    /** 手动触发 bootstrap (仓库为空时把库里函数代码落成文件并首次提交) */
    @PostMapping("/bootstrap")
    public R<Map<String, Object>> bootstrap() {
        try {
            return R.ok(repo.bootstrapIfEmpty());
        } catch (Exception e) {
            return R.error(500, "初始化失败: " + e.getMessage());
        }
    }
}
