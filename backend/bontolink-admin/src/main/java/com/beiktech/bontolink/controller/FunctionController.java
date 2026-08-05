package com.beiktech.bontolink.controller;

import com.beiktech.bontolink.common.R;
import com.beiktech.bontolink.data.mapper.FunctionMapper;
import com.beiktech.bontolink.service.FnRepoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 函数 (Functions) REST 接口
 * <p>
 * 路径: /api/functions 。P0 覆盖主表 CRUD + 状态切换 + 批量删除 + 目录树 +
 * 版本列表 + 运行配置 / 环境变量 / 参数说明 的保存 + 调用统计聚合。
 * <p>
 * 列表默认<b>每个 full_access_path 只返回最新版本</b>一条 (历史版本走详情页顶部
 * 版本选择器)。带 allVersions=true 时返回全部版本记录。
 */
@RestController
@RequestMapping("/api/functions")
public class FunctionController {

    @Autowired private FunctionMapper mapper;
    /** 发布时要把当前分支与 commit 钉进版本库记录;代码仓不可用时降级为不记录 */
    @Autowired(required = false) private FnRepoService fnRepo;

    /** api_name 命名规则: 小驼峰 */
    private static final String API_NAME_RE = "^[a-z][a-zA-Z0-9]*$";

    /* ==================== 列表 ==================== */

    @GetMapping
    public R<List<Map<String, Object>>> list(@RequestParam(required = false, defaultValue = "false") boolean allVersions) {
        List<Map<String, Object>> rows = mapper.listAll();

        // 参数与调用统计一次取回, 按 function_id 分组, 避免 N+1
        Map<String, List<Map<String, Object>>> paramsByFn = groupBy(mapper.listAllParams(), "function_id");
        Map<String, List<Map<String, Object>>> statsByFn = groupBy(mapper.listAllCallStats(), "function_id");
        String since7d = LocalDate.now().minusDays(6).toString();

        // 同一访问路径的版本数
        Map<String, Long> versionCount = rows.stream()
            .collect(Collectors.groupingBy(r -> str(r.get("full_access_path")), Collectors.counting()));

        Map<String, String> classIdByApi = classIdByApiName();
        for (Map<String, Object> r : rows) {
            List<Map<String, Object>> ps = paramsByFn.get(str(r.get("id")));
            resolveObjectClassIds(ps, classIdByApi);
            enrich(r, ps, statsByFn.get(str(r.get("id"))), since7d);
            r.put("version_count", versionCount.getOrDefault(str(r.get("full_access_path")), 1L));
        }
        return R.ok(allVersions ? rows : latestPerPath(rows));
    }

    /** 每个 full_access_path 保留版本号最大的一条, 并标记 is_latest */
    private List<Map<String, Object>> latestPerPath(List<Map<String, Object>> rows) {
        Map<String, Map<String, Object>> best = new LinkedHashMap<>();
        for (Map<String, Object> r : rows) {
            String path = str(r.get("full_access_path"));
            Map<String, Object> cur = best.get(path);
            if (cur == null || compareVersion(str(r.get("version_no")), str(cur.get("version_no"))) > 0) {
                best.put(path, r);
            }
        }
        List<Map<String, Object>> out = new ArrayList<>(best.values());
        out.forEach(r -> r.put("is_latest", 1));
        // 保持"最近更新倒序"
        out.sort((a, b) -> str(b.get("update_time")).compareTo(str(a.get("update_time"))));
        return out;
    }

    /** 语义化版本比较: v1.10.0 > v1.9.3; 非法段按 0 处理 */
    private int compareVersion(String a, String b) {
        String[] xs = a.replaceFirst("^[vV]", "").split("\\.");
        String[] ys = b.replaceFirst("^[vV]", "").split("\\.");
        for (int i = 0; i < Math.max(xs.length, ys.length); i++) {
            int x = i < xs.length ? parseIntSafe(xs[i]) : 0;
            int y = i < ys.length ? parseIntSafe(ys[i]) : 0;
            if (x != y) return Integer.compare(x, y);
        }
        return 0;
    }

    /** 给列表行补聚合字段: 入参出参类型串 / 近7天与总调用量 / 作用对象 */
    private void enrich(Map<String, Object> r, List<Map<String, Object>> params,
                        List<Map<String, Object>> stats, String since7d) {
        List<String> inTypes = new ArrayList<>(), outTypes = new ArrayList<>();
        String subjectType = null;
        String subjectClassId = null;
        if (params != null) {
            params.sort(Comparator.comparingInt(p -> toInt(p.get("sort_num"))));
            for (Map<String, Object> p : params) {
                String type = str(p.get("param_type"));
                if (toInt(p.get("param_direction")) == 2) {
                    outTypes.add(type);
                } else {
                    inTypes.add(type);
                    // 作用对象 = 第一个本体对象类型入参 (形如 "[Hydro] Station")
                    if (subjectType == null && type.startsWith("[")) {
                        subjectType = type;
                        subjectClassId = str(p.get("object_class_id"));
                    }
                }
            }
        }
        r.put("in_types", inTypes);
        r.put("out_types", outTypes);
        r.put("param_count", inTypes.size() + outTypes.size());
        r.put("subject_type", subjectType);
        r.put("subject_class_id", subjectClassId);

        long total = 0, recent = 0;
        if (stats != null) {
            for (Map<String, Object> s : stats) {
                int c = toInt(s.get("call_count"));
                total += c;
                if (str(s.get("stat_date")).compareTo(since7d) >= 0) recent += c;
            }
        }
        r.put("calls_total", total);
        r.put("calls_7d", recent);
    }

    /* ==================== 详情 ==================== */

    /** 详情: 主表 + 参数 + 运行配置 + 环境变量 + 版本列表 + 调用统计概要 */
    @GetMapping("/{id}")
    public R<Map<String, Object>> get(@PathVariable String id) {
        Map<String, Object> row = mapper.findById(id);
        if (row == null) return R.ok(null);
        List<Map<String, Object>> params = mapper.listParams(id);
        resolveObjectClassIds(params, classIdByApiName());
        List<Map<String, Object>> stats = mapper.listCallStats(id);
        enrich(row, new ArrayList<>(params), stats, LocalDate.now().minusDays(6).toString());
        row.put("params", params);
        row.put("in_params", params.stream().filter(p -> toInt(p.get("param_direction")) != 2).toList());
        row.put("out_params", params.stream().filter(p -> toInt(p.get("param_direction")) == 2).toList());
        row.put("runtime_config", mapper.getRuntimeConfig(id));
        row.put("env_vars", mapper.listEnvVars(id));
        row.put("versions", mapper.listVersionsByPath(str(row.get("full_access_path"))).stream()
            .map(v -> Map.<String, Object>of(
                "id", v.get("id"),
                "version_no", v.get("version_no"),
                "status", v.get("status"),
                "publish_time", v.get("publish_time") == null ? "" : v.get("publish_time")))
            .sorted((a, b) -> compareVersion(str(b.get("version_no")), str(a.get("version_no"))))
            .toList());
        return R.ok(row);
    }

    /** 同访问路径的全部版本 (顶部版本选择器单独拉取时用) */
    @GetMapping("/{id}/versions")
    public R<List<Map<String, Object>>> versions(@PathVariable String id) {
        Map<String, Object> row = mapper.findById(id);
        if (row == null) return R.error(404, "未找到函数");
        List<Map<String, Object>> list = mapper.listVersionsByPath(str(row.get("full_access_path")));
        list.sort((a, b) -> compareVersion(str(b.get("version_no")), str(a.get("version_no"))));
        return R.ok(list);
    }

    /* ==================== 创建 / 更新 / 删除 ==================== */

    @PostMapping
    @SuppressWarnings("unchecked")
    public R<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        String apiName = str(body.get("api_name")).trim();
        if (apiName.isEmpty()) return R.error(400, "API 名称 (api_name) 不能为空");
        if (!apiName.matches(API_NAME_RE)) return R.error(400, "API 名称需为小驼峰: 首字母小写, 只含字母与数字");

        String industry = str(body.get("industry_dir")).trim();
        String category = str(body.get("category_dir")).trim();
        if (industry.isEmpty() || category.isEmpty()) return R.error(400, "行业目录与领域目录不能为空");

        String filePath = str(body.get("code_file_path")).trim();
        if (filePath.isEmpty()) return R.error(400, "代码文件路径不能为空");

        // 同文件内 API 重名拦截 (文档 5.2 七、同名函数校验)
        if (mapper.findIdByFileAndApiName(filePath, apiName) != null)
            return R.error(400, "文件 " + filePath + " 内已存在同名 API: " + apiName);

        String version = str(body.getOrDefault("version_no", "")).trim();
        if (version.isEmpty()) version = "v0.0.1";
        String className = str(body.get("class_name")).trim();
        String fullPath = buildFullPath(industry, category, className, apiName);
        if (mapper.existsByPathVersion(fullPath, version) != null)
            return R.error(400, "版本 " + version + " 下访问路径已存在: " + fullPath);

        String id = "ont_function-" + UUID.randomUUID();
        body.put("id", id);
        body.put("api_name", apiName);
        body.put("version_no", version);
        body.put("industry_dir", industry);
        body.put("category_dir", category);
        body.put("class_name", className.isEmpty() ? null : className);
        body.put("full_access_path", fullPath);
        body.put("code_file_path", filePath);
        body.put("rid", "ri.ont.function." + UUID.randomUUID().toString().replace("-", ""));
        // function_label 表上非空: 未填中文名时回落 api_name
        String label = str(body.get("function_label")).trim();
        body.put("function_label", label.isEmpty() ? apiName : label);
        body.putIfAbsent("function_type", 1);
        body.putIfAbsent("language", 2);
        body.putIfAbsent("status", 1);            // 1草稿
        body.putIfAbsent("visibility", 1);        // 1全平台可见
        body.put("is_deleted", 0);
        for (String k : new String[]{"code_md5", "code_content", "file_line_start", "file_line_end",
                "rdfs_label", "rdfs_comment", "rdfs_see_also", "rdfs_defined_by", "create_user", "publish_time"}) {
            body.putIfAbsent(k, null);
        }
        // 代码指纹: 向导只送模板代码, md5 由服务端算, 保证与内容一致
        if (str(body.get("code_md5")).isEmpty() && !str(body.get("code_content")).isEmpty())
            body.put("code_md5", md5(str(body.get("code_content"))));
        // rdfs_label 缺省跟随中文名, 保持本体标准属性不为空
        if (str(body.get("rdfs_label")).isEmpty()) body.put("rdfs_label", body.get("function_label"));
        mapper.insert(body);

        // 目录不存在时自动建版本库记录 (文档 5.2 七、目录自动创建)
        ensureVersionRepo(industry, category, version, str(body.get("create_user")));
        // 参数 (向导第三步)
        saveParams(id, asList(body.get("params")));
        // 运行配置: 未传则落一条默认值
        saveRuntimeConfig(id, (Map<String, Object>) body.get("runtime_config"));
        saveEnvVars(id, asList(body.get("env_vars")));
        return R.ok(mapper.findById(id));
    }

    /**
     * 更新。详情页编辑模式只允许改平台元数据 (中文名 / 说明 / 分类归属 / 可见性),
     * 代码派生字段 (api_name / version_no / 路径 / 代码 / 参数名与类型) 请求未携带时一律沿用旧值。
     */
    @PutMapping("/{id}")
    @SuppressWarnings("unchecked")
    public R<?> update(@PathVariable String id, @RequestBody Map<String, Object> body) {
        Map<String, Object> old = mapper.findById(id);
        if (old == null) return R.error(404, "未找到函数");
        body.put("id", id);
        // 全字段: 请求未携带该键时保留旧值, 避免 partial PUT 误清空
        for (String k : new String[]{"rid", "version_no", "api_name", "function_label", "function_type",
                "language", "industry_dir", "category_dir", "class_name", "full_access_path",
                "code_file_path", "code_md5", "code_content", "file_line_start", "file_line_end",
                "status", "visibility", "rdfs_label", "rdfs_comment", "rdfs_see_also",
                "rdfs_defined_by", "create_user", "publish_time"}) {
            if (!body.containsKey(k)) body.put(k, old.get(k));
        }
        // 分类归属改了要同步重算访问路径
        String industry = str(body.get("industry_dir"));
        String category = str(body.get("category_dir"));
        String className = str(body.get("class_name"));
        String apiName = str(body.get("api_name"));
        String newPath = buildFullPath(industry, category, className, apiName);
        if (!newPath.equals(str(old.get("full_access_path")))) {
            if (mapper.existsByPathVersion(newPath, str(body.get("version_no"))) != null)
                return R.error(400, "目标分类下该版本已存在同名函数: " + newPath);
            body.put("full_access_path", newPath);
            ensureVersionRepo(industry, category, str(body.get("version_no")), str(body.get("create_user")));
        }
        mapper.update(body);

        // 子表: 仅当请求携带对应键时整体覆盖
        if (body.containsKey("params")) {
            mapper.deleteParamsByFunction(id);
            saveParams(id, asList(body.get("params")));
        }
        if (body.containsKey("runtime_config")) {
            saveRuntimeConfig(id, (Map<String, Object>) body.get("runtime_config"));
        }
        if (body.containsKey("env_vars")) {
            mapper.deleteEnvVarsByFunction(id);
            saveEnvVars(id, asList(body.get("env_vars")));
        }
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<?> remove(@PathVariable String id) {
        cascadeDelete(id);
        return R.ok();
    }

    @PostMapping("/batch-delete")
    public R<?> batchDelete(@RequestBody Map<String, Object> body) {
        List<?> ids = body.get("ids") instanceof List<?> l ? l : Collections.emptyList();
        for (Object id : ids) cascadeDelete(String.valueOf(id));
        return R.ok(Map.of("deleted", ids.size()));
    }

    /**
     * 状态切换: 1草稿 / 2已发布 / 3已停用 / 4已废弃。
     * 置为「已发布」请走 {@link #publish} —— 那里才会落发布时间与版本库记录。
     */
    @PostMapping("/{id}/status")
    public R<?> setStatus(@PathVariable String id, @RequestBody Map<String, Object> body) {
        int status = toInt(body.get("status"));
        if (status < 1 || status > 4) return R.error(400, "状态取值必须在 1~4 之间");
        if (status == 2) return publish(id, body);
        if (status == 1) mapper.markDraft(id);      // 撤回草稿连发布时间一起清
        else mapper.updateStatus(id, status);       // 停用 / 废弃保留发布历史
        return R.ok();
    }

    /**
     * 发布函数(遗留 2)。
     * <p>
     * 原先「发布」只改一个 status 字段, 详情页的发布时间永远显示"未发布"。
     * 完整发布要做三件事:
     * <ol>
     *   <li>状态置「已发布」并落 publish_time / publish_user</li>
     *   <li>版本库(ont_version_repo)登记该行业+领域+版本, 状态置「已发布」</li>
     *   <li>把当前代码仓的分支与 commit 钉进版本库记录 —— 发布出去的到底是哪份代码, 要可追溯</li>
     * </ol>
     */
    @PostMapping("/{id}/publish")
    public R<?> publish(@PathVariable String id, @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> fn = mapper.findById(id);
        if (fn == null) return R.error(404, "未找到函数");
        Map<String, Object> b = body == null ? Map.of() : body;
        String user = str(b.get("publish_user"));
        if (user.isEmpty()) user = str(fn.get("create_user"));
        String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault()).format(Instant.now());

        mapper.markPublished(id, now, user);
        Map<String, Object> repoRow = upsertVersionRepo(fn, user, now, str(b.get("release_note")));

        Map<String, Object> row = mapper.findById(id);
        List<Map<String, Object>> params = mapper.listParams(id);
        enrich(row, new ArrayList<>(params), mapper.listCallStats(id),
               LocalDate.now().minusDays(6).toString());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("function", row);
        out.put("version_repo", repoRow);
        return R.ok(out);
    }

    /** 版本库登记:同 行业+领域+版本 已有记录就更新, 没有就新建 */
    private Map<String, Object> upsertVersionRepo(Map<String, Object> fn, String user, String now, String note) {
        String industry = str(fn.get("industry_dir"));
        String category = str(fn.get("category_dir"));
        String version = str(fn.get("version_no"));
        Map<String, Object> repoStatus = fnRepo == null ? Map.of() : fnRepo.status();
        String branch = str(repoStatus.get("current_branch"));
        String commit = str(repoStatus.get("head"));
        String url = str(repoStatus.get("remote"));

        Map<String, Object> exist = mapper.findRepoByDirVersion(industry, category, version);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("industry_dir", industry);
        row.put("category_dir", category);
        row.put("version_no", version);
        row.put("repo_branch", branch);
        row.put("repo_commit_id", commit);
        row.put("repo_url", url.isEmpty() ? null : url);
        row.put("version_status", 3);                 // 3 = 已发布
        row.put("release_note", note.isEmpty() ? (exist == null ? null : exist.get("release_note")) : note);
        row.put("publish_user", user.isEmpty() ? null : user);
        row.put("publish_time", now);
        if (exist != null) {
            row.put("id", exist.get("id"));
            row.put("is_default", exist.get("is_default"));
            mapper.updateRepo(row);
            return mapper.findRepoById(str(exist.get("id")));
        }
        String rid = "ri.ont.version_repo." + UUID.randomUUID().toString().replace("-", "");
        String rowId = "ont_version_repo-" + UUID.randomUUID();
        row.put("id", rowId);
        row.put("rid", rid);
        row.put("is_default", 0);
        mapper.insertRepo(row);
        return mapper.findRepoById(rowId);
    }

    /** 批量发布(列表页的批量「发布」按钮) */
    @PostMapping("/batch-publish")
    public R<?> batchPublish(@RequestBody Map<String, Object> body) {
        List<?> ids = body.get("ids") instanceof List<?> l ? l : Collections.emptyList();
        int ok = 0;
        for (Object id : ids) {
            Map<String, Object> fn = mapper.findById(String.valueOf(id));
            if (fn == null) continue;
            publish(String.valueOf(id), body);
            ok++;
        }
        return R.ok(Map.of("published", ok));
    }

    /* ==================== 子配置单独保存 (详情页配置 Tab) ==================== */

    @PutMapping("/{id}/runtime-config")
    public R<?> saveRuntime(@PathVariable String id, @RequestBody Map<String, Object> body) {
        if (mapper.findById(id) == null) return R.error(404, "未找到函数");
        String err = validateRuntime(body);
        if (err != null) return R.error(400, err);
        saveRuntimeConfig(id, body);
        return R.ok(mapper.getRuntimeConfig(id));
    }

    @PutMapping("/{id}/env-vars")
    public R<?> saveEnv(@PathVariable String id, @RequestBody Map<String, Object> body) {
        if (mapper.findById(id) == null) return R.error(404, "未找到函数");
        mapper.deleteEnvVarsByFunction(id);
        saveEnvVars(id, asList(body.get("env_vars")));
        return R.ok(mapper.listEnvVars(id));
    }

    /** 参数说明批量保存 (代码派生的名称/类型只读, 只允许改 param_desc) */
    @PutMapping("/{id}/param-desc")
    public R<?> saveParamDesc(@PathVariable String id, @RequestBody Map<String, Object> body) {
        for (Map<String, Object> p : asList(body.get("params"))) {
            String pid = str(p.get("id"));
            if (!pid.isEmpty()) mapper.updateParamDesc(pid, p.get("param_desc"));
        }
        return R.ok(mapper.listParams(id));
    }

    /* ==================== 目录 / 文件 / 校验 ==================== */

    /** 左侧「行业领域分组」树: 全部函数 → 行业大类 → 业务子域 */
    @GetMapping("/dirs")
    public R<List<Map<String, Object>>> dirs() {
        // 目录树按"列表可见口径"计数, 即每个访问路径只算最新版本一条
        List<Map<String, Object>> latest = latestPerPath(mapper.listAll());
        Map<String, Map<String, Integer>> agg = new LinkedHashMap<>();
        for (Map<String, Object> f : latest) {
            agg.computeIfAbsent(str(f.get("industry_dir")), k -> new LinkedHashMap<>())
               .merge(str(f.get("category_dir")), 1, Integer::sum);
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map.Entry<String, Map<String, Integer>> e : agg.entrySet()) {
            List<Map<String, Object>> children = new ArrayList<>();
            int total = 0;
            for (Map.Entry<String, Integer> c : e.getValue().entrySet()) {
                children.add(new LinkedHashMap<>(Map.of("category_dir", c.getKey(), "count", c.getValue())));
                total += c.getValue();
            }
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("industry_dir", e.getKey());
            node.put("count", total);
            node.put("children", children);
            out.add(node);
        }
        return R.ok(out);
    }

    /** 已有代码文件清单 (向导「文件」选择弹窗) */
    @GetMapping("/files")
    public R<List<Map<String, Object>>> files() { return R.ok(mapper.listCodeFiles()); }

    /** 实时重名检测: 文件是否已存在 / 同文件内 API 是否重名 */
    @GetMapping("/check-api-name")
    public R<Map<String, Object>> checkApiName(@RequestParam String filePath,
                                               @RequestParam String apiName,
                                               @RequestParam(required = false) String excludeId) {
        Map<String, Object> out = new LinkedHashMap<>();
        boolean nameOk = apiName != null && apiName.matches(API_NAME_RE);
        String hitId = (nameOk && filePath != null && !filePath.isBlank())
            ? mapper.findIdByFileAndApiName(filePath, apiName) : null;
        boolean dup = hitId != null && !hitId.equals(excludeId);
        out.put("name_valid", nameOk);
        out.put("duplicated", dup);
        out.put("file_exists", mapper.listCodeFiles().stream()
            .anyMatch(f -> Objects.equals(str(f.get("code_file_path")), filePath)));
        out.put("message", !nameOk ? "API 名称需为小驼峰: 首字母小写, 只含字母与数字"
            : dup ? "该文件内已存在同名 API" : "");
        return R.ok(out);
    }

    /* ==================== 调用统计 (可观测性 Tab) ==================== */

    @GetMapping("/{id}/stats")
    public R<Map<String, Object>> stats(@PathVariable String id,
                                        @RequestParam(required = false, defaultValue = "30") int days) {
        String since = LocalDate.now().minusDays(Math.max(1, days) - 1L).toString();
        List<Map<String, Object>> rows = mapper.listCallStats(id);

        long total = 0, success = 0, error = 0, costWeighted = 0;
        Map<String, long[]> byDate = new TreeMap<>();     // date -> [calls, errors]
        Map<String, Long> byCaller = new LinkedHashMap<>();
        for (Map<String, Object> s : rows) {
            int calls = toInt(s.get("call_count"));
            total += calls;
            success += toInt(s.get("success_count"));
            error += toInt(s.get("error_count"));
            costWeighted += (long) toInt(s.get("avg_cost_ms")) * calls;
            byCaller.merge(str(s.get("caller_app")), (long) calls, Long::sum);
            String d = str(s.get("stat_date"));
            if (d.compareTo(since) >= 0) {
                long[] cell = byDate.computeIfAbsent(d, k -> new long[2]);
                cell[0] += calls;
                cell[1] += toInt(s.get("error_count"));
            }
        }
        List<Map<String, Object>> trend = byDate.entrySet().stream()
            .map(e -> Map.<String, Object>of("date", e.getKey(), "calls", e.getValue()[0], "errors", e.getValue()[1]))
            .toList();
        final long finalTotal = total;
        List<Map<String, Object>> callers = byCaller.entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
            .map(e -> Map.<String, Object>of(
                "app", e.getKey(), "calls", e.getValue(),
                "ratio", finalTotal == 0 ? 0d : Math.round(e.getValue() * 10000.0 / finalTotal) / 100.0))
            .toList();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total_calls", total);
        out.put("success_rate", total == 0 ? 0d : Math.round(success * 10000.0 / total) / 100.0);
        out.put("avg_cost_ms", total == 0 ? 0 : Math.round((double) costWeighted / total));
        out.put("error_count", error);
        out.put("days", days);
        out.put("trend", trend);
        out.put("callers", callers);
        return R.ok(out);
    }

    /* ==================== 内部工具 ==================== */

    /** api_name(小写) → 对象类 id */
    private Map<String, String> classIdByApiName() {
        Map<String, String> m = new HashMap<>();
        for (Map<String, Object> c : mapper.listClassApiNames()) {
            m.put(str(c.get("api_name")).toLowerCase(), str(c.get("id")));
        }
        return m;
    }

    /**
     * 给参数补 object_class_id:向导没绑过对象类时, 用参数类型里的裸类名
     * ("[命名空间] Station" → "Station") 去对象类库反查, 让详情页的类型链接可跳转。
     */
    private void resolveObjectClassIds(List<Map<String, Object>> params, Map<String, String> classIdByApi) {
        if (params == null) return;
        for (Map<String, Object> p : params) {
            if (!str(p.get("object_class_id")).isEmpty()) continue;
            String type = str(p.get("param_type"));
            if (!type.startsWith("[")) continue;
            String bare = type.replaceFirst("^\\s*\\[[^\\]]*\\]\\s*", "").trim();
            String hit = classIdByApi.get(bare.toLowerCase());
            if (hit != null) p.put("object_class_id", hit);
        }
    }

    /** /行业/领域/类名/方法名 ; 类名为空时省略该段 */
    private String buildFullPath(String industry, String category, String className, String apiName) {
        StringBuilder sb = new StringBuilder("/").append(industry).append('/').append(category);
        if (className != null && !className.isBlank()) sb.append('/').append(className);
        return sb.append('/').append(apiName).toString();
    }

    /** 行业/领域/版本不存在时自动补一条版本库记录 */
    private void ensureVersionRepo(String industry, String category, String version, String user) {
        if (mapper.findRepoByDirVersion(industry, category, version) != null) return;
        Map<String, Object> repo = new LinkedHashMap<>();
        repo.put("id", "ont_version_repo-" + UUID.randomUUID());
        repo.put("rid", "ri.ont.version_repo." + UUID.randomUUID().toString().replace("-", ""));
        repo.put("industry_dir", industry);
        repo.put("category_dir", category);
        repo.put("version_no", version);
        repo.put("repo_branch", "main");
        repo.put("repo_commit_id", "");
        repo.put("repo_url", null);
        repo.put("version_status", 1);      // 1草稿中
        repo.put("is_default", 0);
        repo.put("release_note", null);
        repo.put("publish_user", user == null || user.isBlank() ? null : user);
        repo.put("publish_time", null);
        try { mapper.insertRepo(repo); } catch (Exception ignore) { /* 并发下唯一索引冲突可忽略 */ }
    }

    private void saveParams(String functionId, List<Map<String, Object>> params) {
        int inSort = 1, outSort = 1;
        for (Map<String, Object> p : params) {
            String name = str(p.get("param_name")).trim();
            String type = str(p.get("param_type")).trim();
            if (name.isEmpty() && type.isEmpty()) continue;
            int direction = toInt(p.getOrDefault("param_direction", 1));
            if (direction != 2) direction = 1;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", "ont_function_param-" + UUID.randomUUID());
            row.put("function_id", functionId);
            row.put("param_name", name.isEmpty() ? (direction == 2 ? "result" : "arg") : name);
            row.put("param_type", type.isEmpty() ? "any" : type);
            row.put("param_direction", direction);
            row.put("is_required", toInt(p.getOrDefault("is_required", direction == 1 ? 1 : 0)));
            row.put("default_value", p.get("default_value"));
            row.put("value_range", p.get("value_range"));
            row.put("param_desc", p.get("param_desc"));
            row.put("object_class_id", p.get("object_class_id"));
            row.put("sort_num", p.containsKey("sort_num") ? toInt(p.get("sort_num")) : (direction == 2 ? outSort++ : inSort++));
            mapper.insertParam(row);
        }
    }

    /** 运行配置: 有则更新, 无则插入 (未传配置时落一条全默认值) */
    private void saveRuntimeConfig(String functionId, Map<String, Object> cfg) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("function_id", functionId);
        row.put("timeout", pick(cfg, "timeout", 30));
        row.put("retry_count", pick(cfg, "retry_count", 2));
        row.put("retry_interval", pick(cfg, "retry_interval", 1));
        row.put("memory_quota", pick(cfg, "memory_quota", 512));
        row.put("concurrency_limit", pick(cfg, "concurrency_limit", 100));
        row.put("enable_cache", pick(cfg, "enable_cache", 1));
        row.put("cache_ttl", pick(cfg, "cache_ttl", 3600));
        if (mapper.getRuntimeConfig(functionId) != null) {
            mapper.updateRuntimeConfig(row);
        } else {
            row.put("id", "ont_function_runtime_config-" + UUID.randomUUID());
            mapper.insertRuntimeConfig(row);
        }
    }

    /** 运行配置取值范围校验 (文档 4.5.3.3) */
    private String validateRuntime(Map<String, Object> cfg) {
        int[][] ranges = {{toInt(pick(cfg, "timeout", 30)), 1, 3600},
                          {toInt(pick(cfg, "retry_count", 2)), 0, 5},
                          {toInt(pick(cfg, "retry_interval", 1)), 0, 60},
                          {toInt(pick(cfg, "memory_quota", 512)), 128, 4096},
                          {toInt(pick(cfg, "concurrency_limit", 100)), 1, 1000},
                          {toInt(pick(cfg, "cache_ttl", 3600)), 60, 86400}};
        String[] labels = {"超时时间(1~3600秒)", "失败重试次数(0~5次)", "重试间隔(0~60秒)",
                           "内存配额(128~4096MB)", "并发限制(1~1000个)", "缓存有效期(60~86400秒)"};
        for (int i = 0; i < ranges.length; i++) {
            if (ranges[i][0] < ranges[i][1] || ranges[i][0] > ranges[i][2]) return labels[i] + " 超出取值范围";
        }
        return null;
    }

    private void saveEnvVars(String functionId, List<Map<String, Object>> vars) {
        int i = 1;
        for (Map<String, Object> v : vars) {
            String name = str(v.get("var_name")).trim();
            if (name.isEmpty()) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", "ont_function_env_var-" + UUID.randomUUID());
            row.put("function_id", functionId);
            row.put("var_name", name);
            row.put("var_value", v.get("var_value") == null ? "" : String.valueOf(v.get("var_value")));
            row.put("var_type", toInt(v.getOrDefault("var_type", 1)));
            row.put("value_range", v.get("value_range"));
            row.put("var_desc", v.get("var_desc"));
            row.put("is_encrypt", toInt(v.getOrDefault("is_encrypt", 0)));
            row.put("sort_num", v.containsKey("sort_num") ? toInt(v.get("sort_num")) : i++);
            mapper.insertEnvVar(row);
        }
    }

    private void cascadeDelete(String id) {
        mapper.deleteParamsByFunction(id);
        mapper.deleteRuntimeConfigByFunction(id);
        mapper.deleteEnvVarsByFunction(id);
        mapper.deleteCallStatsByFunction(id);
        mapper.delete(id);
    }

    private Map<String, List<Map<String, Object>>> groupBy(List<Map<String, Object>> rows, String key) {
        Map<String, List<Map<String, Object>>> out = new HashMap<>();
        for (Map<String, Object> r : rows) out.computeIfAbsent(str(r.get(key)), k -> new ArrayList<>()).add(r);
        return out;
    }

    private Object pick(Map<String, Object> cfg, String key, Object dft) {
        if (cfg == null) return dft;
        Object v = cfg.get(key);
        return v == null || String.valueOf(v).isBlank() ? dft : v;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asList(Object raw) {
        return raw instanceof List ? (List<Map<String, Object>>) raw : Collections.emptyList();
    }
    /** 源码 MD5 指纹 (文档 4.2 code_md5, 用于版本一致性校验) */
    private String md5(String text) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("MD5")
                .digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String str(Object v) { return v == null ? "" : String.valueOf(v); }
    private int toInt(Object v) {
        if (v instanceof Number n) return n.intValue();
        if (v instanceof Boolean b) return b ? 1 : 0;
        return parseIntSafe(String.valueOf(v));
    }
    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }
}
