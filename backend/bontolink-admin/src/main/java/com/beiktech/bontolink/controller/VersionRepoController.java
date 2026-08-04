package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.data.mapper.FunctionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 函数版本库 (ont_version_repo) REST 接口
 * <p>
 * 路径: /api/version-repos 。版本库以「行业目录 + 领域目录」为维度管理函数版本序列,
 * 主要供新建向导的目录下拉与详情页版本信息使用;函数创建时若目录/版本缺失会由
 * {@link FunctionController} 自动补建。
 */
@RestController
@RequestMapping("/api/version-repos")
public class VersionRepoController {

    @Autowired private FunctionMapper mapper;

    /** 全部版本库 */
    @GetMapping
    public R<List<Map<String, Object>>> list() { return R.ok(mapper.listRepos()); }

    @GetMapping("/{id}")
    public R<Map<String, Object>> get(@PathVariable String id) { return R.ok(mapper.findRepoById(id)); }

    /** 行业 → 领域 两级目录选项 (新建向导目录下拉 / 目录树选择弹窗) */
    @GetMapping("/dir-options")
    public R<List<Map<String, Object>>> dirOptions() {
        Map<String, Set<String>> agg = new LinkedHashMap<>();
        for (Map<String, Object> r : mapper.listRepos()) {
            agg.computeIfAbsent(String.valueOf(r.get("industry_dir")), k -> new LinkedHashSet<>())
               .add(String.valueOf(r.get("category_dir")));
        }
        // 已有函数但版本库缺失时兜底补进来
        for (Map<String, Object> r : mapper.listDirCounts()) {
            agg.computeIfAbsent(String.valueOf(r.get("industry_dir")), k -> new LinkedHashSet<>())
               .add(String.valueOf(r.get("category_dir")));
        }
        List<Map<String, Object>> out = new ArrayList<>();
        agg.forEach((industry, categories) -> {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("industry_dir", industry);
            node.put("categories", new ArrayList<>(categories));
            out.add(node);
        });
        return R.ok(out);
    }

    @PostMapping
    public R<?> create(@RequestBody Map<String, Object> body) {
        String industry = trim(body.get("industry_dir"));
        String category = trim(body.get("category_dir"));
        String version = trim(body.get("version_no"));
        if (industry.isEmpty() || category.isEmpty() || version.isEmpty())
            return R.error(400, "行业目录 / 领域目录 / 版本号均不能为空");
        if (mapper.findRepoByDirVersion(industry, category, version) != null)
            return R.error(400, "该行业领域下版本号已存在: " + version);

        String id = "ont_version_repo-" + UUID.randomUUID();
        body.put("id", id);
        body.put("industry_dir", industry);
        body.put("category_dir", category);
        body.put("version_no", version);
        body.put("rid", "ri.ont.version_repo." + UUID.randomUUID().toString().replace("-", ""));
        body.putIfAbsent("repo_branch", "main");
        body.putIfAbsent("repo_commit_id", "");
        body.putIfAbsent("version_status", 1);
        body.putIfAbsent("is_default", 0);
        for (String k : new String[]{"repo_url", "release_note", "publish_user", "publish_time"}) body.putIfAbsent(k, null);
        mapper.insertRepo(body);
        return R.ok(mapper.findRepoById(id));
    }

    @PutMapping("/{id}")
    public R<?> update(@PathVariable String id, @RequestBody Map<String, Object> body) {
        Map<String, Object> old = mapper.findRepoById(id);
        if (old == null) return R.error(404, "未找到版本库");
        body.put("id", id);
        for (String k : new String[]{"repo_branch", "repo_commit_id", "repo_url", "version_status",
                "is_default", "release_note", "publish_user", "publish_time"}) {
            if (!body.containsKey(k)) body.put(k, old.get(k));
        }
        mapper.updateRepo(body);
        return R.ok(mapper.findRepoById(id));
    }

    private String trim(Object v) { return v == null ? "" : String.valueOf(v).trim(); }
}
