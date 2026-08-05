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

    /** 一次提交的详情:元信息 + 变更文件清单(含增删行数) */
    @GetMapping("/commit")
    public R<Map<String, Object>> commit(@RequestParam String commit) {
        try {
            return R.ok(repo.commitDetail(commit));
        } catch (Exception e) {
            return R.error(400, "读取提交失败: " + e.getMessage());
        }
    }

    /** 某文件在这次提交前后的两份正文, 供 diff 编辑器左右两栏 */
    @GetMapping("/commit-file")
    public R<Map<String, Object>> commitFile(@RequestParam String commit, @RequestParam String path) {
        try {
            return R.ok(repo.commitFile(commit, path));
        } catch (Exception e) {
            return R.error(400, "读取对比内容失败: " + e.getMessage());
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

    /* ==================== 分支管理 (IDE 左侧「分支管理」面板) ==================== */

    @GetMapping("/branches")
    public R<List<Map<String, Object>>> branches() {
        try {
            return R.ok(repo.branches());
        } catch (Exception e) {
            return R.error(500, "读取分支失败: " + e.getMessage());
        }
    }

    /** 新建分支(基于当前 HEAD);checkout=true 时立即切过去 */
    @PostMapping("/branches")
    public R<Map<String, Object>> createBranch(@RequestBody Map<String, Object> body) {
        String name = String.valueOf(body.getOrDefault("name", "")).trim();
        boolean checkout = !"false".equalsIgnoreCase(String.valueOf(body.getOrDefault("checkout", true)));
        try {
            return R.ok(repo.createBranch(name, checkout));
        } catch (Exception e) {
            return R.error(400, e.getMessage());
        }
    }

    /** 删除本地分支;force=true 用于删除有未合并提交的分支(前端二次确认后才带) */
    @DeleteMapping("/branches")
    public R<Map<String, Object>> deleteBranch(@RequestParam String name,
                                               @RequestParam(required = false, defaultValue = "false") boolean force) {
        try {
            return R.ok(repo.deleteBranch(name, force));
        } catch (Exception e) {
            return R.error(400, e.getMessage());
        }
    }

    /** 切换分支(工作区不干净时拒绝) */
    @PostMapping("/checkout")
    public R<Map<String, Object>> checkout(@RequestBody Map<String, Object> body) {
        String name = String.valueOf(body.getOrDefault("name", "")).trim();
        try {
            return R.ok(repo.checkoutBranch(name));
        } catch (Exception e) {
            return R.error(400, e.getMessage());
        }
    }

    /** 把指定分支合并进当前分支;冲突会自动回滚并返回冲突文件清单 */
    @PostMapping("/merge")
    public R<Map<String, Object>> merge(@RequestBody Map<String, Object> body) {
        String name = String.valueOf(body.getOrDefault("name", "")).trim();
        boolean noFf = "true".equalsIgnoreCase(String.valueOf(body.getOrDefault("no_ff", false)));
        try {
            return R.ok(repo.mergeBranch(name, noFf));
        } catch (Exception e) {
            return R.error(400, e.getMessage());
        }
    }

    /* ==================== 未提交改动 ==================== */

    @GetMapping("/changes")
    public R<Map<String, Object>> changes() {
        try {
            return R.ok(repo.changes());
        } catch (Exception e) {
            return R.error(500, "读取改动失败: " + e.getMessage());
        }
    }

    /** 撤销未提交的改动;不传 path = 全部撤销 */
    @PostMapping("/discard")
    public R<Map<String, Object>> discard(@RequestBody(required = false) Map<String, Object> body) {
        String path = body == null ? null : String.valueOf(body.getOrDefault("path", "")).trim();
        try {
            return R.ok(repo.discard(path == null || path.isEmpty() ? null : path));
        } catch (Exception e) {
            return R.error(400, "撤销失败: " + e.getMessage());
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
