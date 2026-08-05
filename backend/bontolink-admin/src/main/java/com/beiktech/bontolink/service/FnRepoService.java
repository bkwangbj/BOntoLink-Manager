package com.beiktech.bontolink.service;

import com.beiktech.bontolink.data.mapper.FunctionMapper;
import jakarta.annotation.PostConstruct;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 函数代码仓服务 (P5)
 * <p>
 * 用 JGit 在服务端维护一份函数代码仓的工作区:克隆 / 读写 / 提交 / 推送 / 历史。
 * 前端 IDE 的项目文件树、编辑器标签页都以这个工作区为唯一文件来源。
 * <p>
 * <b>凭据</b>:仓库地址、账号、token 一律从环境变量注入 (见 application.yml 的
 * bontolink.fn-repo.*),代码与配置文件里不落任何明文凭据。
 * <p>
 * <b>启动策略</b>:初始化在后台线程做,克隆慢或网络不通都不阻塞应用启动;
 * 失败只置状态 + 记日志,IDE 侧据此提示"仓库不可用"。
 */
@Service
public class FnRepoService {

    private static final Logger log = LoggerFactory.getLogger(FnRepoService.class);

    @Autowired private FunctionMapper functionMapper;

    @Value("${bontolink.fn-repo.enabled:true}")      private boolean enabled;
    @Value("${bontolink.fn-repo.url:}")              private String url;
    @Value("${bontolink.fn-repo.branch:master}")     private String branch;
    @Value("${bontolink.fn-repo.workdir:../fn-repo}") private String workdir;
    @Value("${bontolink.fn-repo.username:}")         private String username;
    @Value("${bontolink.fn-repo.token:}")            private String token;
    @Value("${bontolink.fn-repo.author-name:BOntoLink}")  private String authorName;
    @Value("${bontolink.fn-repo.author-email:bontolink@local}") private String authorEmail;
    @Value("${bontolink.fn-repo.auto-push:true}")    private boolean autoPush;
    @Value("${bontolink.fn-repo.bootstrap:true}")    private boolean bootstrapEnabled;

    /** 工作区根目录 (绝对路径, 规范化) */
    private Path root;
    /** 就绪状态:未就绪时所有读写接口直接报错, 不做静默降级 */
    private volatile boolean ready = false;
    private volatile String lastError = null;
    /** 工作区 .git/config 里的 origin 地址 —— 环境变量没配时拿它兜底 */
    private volatile String workdirRemote = null;

    /**
     * 实际生效的远程地址。
     * 配置(环境变量)优先;没配就用工作区里已有的 origin。
     * 这样"启动时忘了带 FN_REPO_URL"不会让一个明明有远程的仓库静默退化成纯本地仓,
     * 而是照常显示远程、照常允许推送(缺凭据时给出明确的鉴权错误)。
     */
    private String effectiveUrl() {
        if (url != null && !url.isBlank()) return url;
        return workdirRemote;
    }

    @PostConstruct
    public void startup() {
        if (!enabled) {
            lastError = "函数代码仓未启用 (bontolink.fn-repo.enabled=false)";
            log.info("[fn-repo] 未启用, 跳过初始化");
            return;
        }
        Thread t = new Thread(this::initSafely, "fn-repo-init");
        t.setDaemon(true);
        t.start();
    }

    private void initSafely() {
        try {
            init();
            ready = true;
            lastError = null;
            log.info("[fn-repo] 就绪: {} (branch={})", root, branch);
            fetchQuietly();     // 补齐远程跟踪引用, 否则「待推送数」算不出来
            if (bootstrapEnabled) bootstrapIfEmpty();
        } catch (Exception e) {
            ready = false;
            lastError = e.getClass().getSimpleName() + ": " + e.getMessage();
            log.error("[fn-repo] 初始化失败: {}", lastError, e);
        }
    }

    /** 打开已有工作区;不存在则克隆(配了 url)或本地新建 */
    private synchronized void init() throws Exception {
        root = Paths.get(workdir).toAbsolutePath().normalize();
        File dotGit = root.resolve(".git").toFile();
        if (dotGit.isDirectory()) {
            try (Git git = open()) {
                log.info("[fn-repo] 复用已有工作区: {}", root);
                workdirRemote = git.getRepository().getConfig()
                    .getString("remote", Constants.DEFAULT_REMOTE_NAME, "url");
                checkoutBranch(git);
            }
            warnIfRemoteWithoutCredentials();
            return;
        }
        Files.createDirectories(root);
        if (url != null && !url.isBlank()) {
            log.info("[fn-repo] 克隆 {} → {}", url, root);
            Git.cloneRepository()
               .setURI(url)
               .setDirectory(root.toFile())
               .setCredentialsProvider(credentials())
               .setCloneAllBranches(false)
               .call()
               .close();
            try (Git git = open()) { checkoutBranch(git); }
        } else {
            log.info("[fn-repo] 未配置远程地址, 在 {} 新建本地仓库", root);
            Git.init().setDirectory(root.toFile()).setInitialBranch(branch).call().close();
        }
    }

    /** 有远程但没凭据 = 推送到点上才会失败, 启动时就喊出来 */
    private void warnIfRemoteWithoutCredentials() {
        String eff = effectiveUrl();
        if (eff == null || eff.isBlank()) return;
        if (url == null || url.isBlank())
            log.warn("[fn-repo] 未配置 FN_REPO_URL, 改用工作区已有的 origin: {}", maskUrl(eff));
        if (token == null || token.isBlank())
            log.warn("[fn-repo] 远程 {} 无访问凭据 (FN_REPO_TOKEN 未配置), 推送会因鉴权失败", maskUrl(eff));
    }

    /** 空仓库(无任何提交)时, 把库里已有函数的代码落成真实文件并首次提交 */
    public synchronized Map<String, Object> bootstrapIfEmpty() throws Exception {
        Map<String, Object> out = new LinkedHashMap<>();
        try (Git git = open()) {
            if (hasCommits(git)) {
                out.put("bootstrapped", false);
                out.put("reason", "仓库已有提交, 跳过 bootstrap");
                return out;
            }
            List<Map<String, Object>> files = buildFilesFromDb();
            for (Map<String, Object> f : files) {
                writeFileRaw(String.valueOf(f.get("path")), String.valueOf(f.get("content")));
            }
            writeFileRaw("README.md", readmeText(files.size()));
            RevCommit commit = commitAll(git, "chore: 由 BOntoLink 平台初始化函数代码仓 (" + files.size() + " 个文件)");
            out.put("bootstrapped", true);
            out.put("files", files.stream().map(f -> f.get("path")).toList());
            out.put("commit", commit.getName());
            if (autoPush) out.put("push", push(git));
            log.info("[fn-repo] bootstrap 完成: {} 个文件, commit={}", files.size(), commit.getName());
        }
        return out;
    }

    /**
     * 按 code_file_path 归集函数代码。
     * 同一路径多版本只取最新版本;同一文件多个函数按源码起始行号排序后拼接。
     */
    private List<Map<String, Object>> buildFilesFromDb() {
        Map<String, Map<String, Object>> latestByPath = new LinkedHashMap<>();
        for (Map<String, Object> f : functionMapper.listAll()) {
            String accessPath = str(f.get("full_access_path"));
            Map<String, Object> cur = latestByPath.get(accessPath);
            if (cur == null || compareVersion(str(f.get("version_no")), str(cur.get("version_no"))) > 0) {
                latestByPath.put(accessPath, f);
            }
        }
        Map<String, List<Map<String, Object>>> byFile = new LinkedHashMap<>();
        for (Map<String, Object> f : latestByPath.values()) {
            String p = str(f.get("code_file_path"));
            if (p.isBlank()) continue;
            byFile.computeIfAbsent(p, k -> new ArrayList<>()).add(f);
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> e : byFile.entrySet()) {
            List<Map<String, Object>> fns = e.getValue();
            fns.sort(Comparator.comparingInt(f -> toInt(f.get("file_line_start"))));
            String body = fns.stream()
                .map(f -> str(f.get("code_content")))
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining("\n\n"));
            if (body.isBlank()) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("path", e.getKey());
            row.put("content", body + "\n");
            out.add(row);
        }
        return out;
    }

    private String readmeText(int fileCount) {
        return String.join("\n",
            "# BOntoLink 函数代码仓",
            "",
            "本仓库由「动态本体管理系统」的函数模块自动维护,内容为平台上登记的函数源码。",
            "",
            "- 目录结构对应函数的 `code_file_path`(代码文件路径)",
            "- 函数的业务归属(行业 / 领域 / 类名 / 方法名)记录在平台的访问路径里,不体现为物理目录",
            "- 初始化时间:" + nowText(),
            "- 初始文件数:" + fileCount,
            "",
            "> 请通过平台的「函数在线编排系统」编辑,直接改动本仓库需要在平台侧重新同步。",
            "");
    }

    /* ==================== 文件读写 ==================== */

    /** 工作区文件树 (跳过 .git);返回嵌套节点 */
    public List<Map<String, Object>> tree() throws IOException {
        requireReady();
        return listDir(root);
    }

    private List<Map<String, Object>> listDir(Path dir) throws IOException {
        List<Map<String, Object>> out = new ArrayList<>();
        if (!Files.isDirectory(dir)) return out;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            List<Path> entries = new ArrayList<>();
            ds.forEach(entries::add);
            entries.sort(Comparator
                .comparing((Path p) -> !Files.isDirectory(p))          // 目录在前
                .thenComparing(p -> p.getFileName().toString().toLowerCase()));
            for (Path p : entries) {
                String name = p.getFileName().toString();
                if (".git".equals(name)) continue;
                Map<String, Object> node = new LinkedHashMap<>();
                node.put("name", name);
                node.put("path", root.relativize(p).toString().replace('\\', '/'));
                boolean dirFlag = Files.isDirectory(p);
                node.put("dir", dirFlag);
                if (dirFlag) node.put("children", listDir(p));
                else node.put("size", Files.size(p));
                out.add(node);
            }
        }
        return out;
    }

    public String read(String relPath) throws IOException {
        requireReady();
        Path p = safeResolve(relPath);
        if (!Files.isRegularFile(p)) throw new IllegalArgumentException("文件不存在: " + relPath);
        return Files.readString(p, StandardCharsets.UTF_8);
    }

    /** 写文件并提交;auto-push 打开时顺带推送 */
    public synchronized Map<String, Object> write(String relPath, String content, String message) throws Exception {
        requireReady();
        writeFileRaw(relPath, content);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("path", relPath);
        try (Git git = open()) {
            String msg = (message == null || message.isBlank())
                ? "chore: 更新 " + relPath + " (BOntoLink IDE)" : message;
            RevCommit commit = commitAll(git, msg);
            out.put("committed", commit != null);
            if (commit == null) out.put("reason", "内容无变化");
            else out.put("commit", commit.getName());
            // 无论有没有产生提交都回写:文件内容没变但库里与磁盘不一致时(比如历史遗留),
            // 保存一次就能把函数记录自愈到与仓库一致
            out.put("synced", syncCodeToDb(git, relPath, content));
            if (commit != null && autoPush) out.put("push", push(git));
        }
        return out;
    }

    /**
     * 把刚保存的源码回写到 ont_function(遗留 1)。
     * <p>
     * 不回写的话,IDE 里改完代码、函数详情页的「代码预览」还是旧的,code_md5 也不再代表真实指纹。
     * <p>
     * <b>只在主干分支回写</b>:在 feature 分支上改的代码不该被当成该函数的正式源码,
     * 否则详情页会展示一份还没合并的实现。
     */
    private Map<String, Object> syncCodeToDb(Git git, String relPath, String content) {
        Map<String, Object> out = new LinkedHashMap<>();
        String path = normalizeRel(relPath);
        String cur = currentBranch(git.getRepository());
        if (!branch.equals(cur)) {
            out.put("ok", false);
            out.put("reason", "当前在 " + cur + " 分支, 只有 " + branch + " 上的改动才回写函数记录");
            return out;
        }
        try {
            List<Map<String, Object>> rows = functionMapper.listByFilePath(path);
            // 同一文件多版本时只回写最新版本那条:历史版本的 code_content 是当时的快照, 不能被改写
            Map<String, Map<String, Object>> latest = new LinkedHashMap<>();
            for (Map<String, Object> r : rows) {
                String key = str(r.get("full_access_path"));
                Map<String, Object> old = latest.get(key);
                if (old == null || str(r.get("version_no")).compareTo(str(old.get("version_no"))) > 0)
                    latest.put(key, r);
            }
            String md5 = md5Hex(content);
            List<String> updated = new ArrayList<>();
            for (Map<String, Object> r : latest.values()) {
                int[] range = locateMember(content, str(r.get("api_name")));
                Map<String, Object> upd = new LinkedHashMap<>();
                upd.put("id", r.get("id"));
                upd.put("code_content", content);
                upd.put("code_md5", md5);
                upd.put("file_line_start", range == null ? r.get("file_line_start") : range[0]);
                upd.put("file_line_end", range == null ? r.get("file_line_end") : range[1]);
                functionMapper.updateCode(upd);
                updated.add(str(r.get("api_name")));
            }
            out.put("ok", true);
            out.put("functions", updated);
        } catch (Exception e) {
            // 回写失败不能让保存失败 —— 文件已经提交了, 这里只是镜像
            out.put("ok", false);
            out.put("reason", e.getMessage());
            log.warn("[fn-repo] 回写函数源码失败 {}: {}", path, e.getMessage());
        }
        return out;
    }

    /**
     * 在源码里定位某个方法的行号区间。
     * 起始行取方法声明上方连续的装饰器/注解行;结束行 TS 按花括号配平、Python 按缩进回落。
     * 定位不到返回 null,调用方保留原值而不是写个错的进去。
     */
    private int[] locateMember(String content, String apiName) {
        if (apiName == null || apiName.isBlank()) return null;
        String[] lines = content.split("\r?\n", -1);
        java.util.regex.Pattern decl = java.util.regex.Pattern.compile(
            "(^|[^\\w$])" + java.util.regex.Pattern.quote(apiName) + "\\s*\\(");
        int idx = -1;
        for (int i = 0; i < lines.length; i++) {
            String t = lines[i];
            boolean isDecl = t.contains("def " + apiName) || t.contains("function " + apiName)
                || (decl.matcher(t).find() && (t.contains("public ") || t.contains("private ")
                    || t.contains("static ") || t.contains("def ") || t.contains("function ")
                    || t.trim().startsWith(apiName)));
            if (isDecl) { idx = i; break; }
        }
        if (idx < 0) return null;

        int start = idx;
        while (start > 0) {
            String prev = lines[start - 1].trim();
            if (prev.startsWith("@")) start--;      // 装饰器算函数的一部分
            else break;
        }
        int end = findMemberEnd(lines, idx);
        return new int[]{ start + 1, end + 1 };     // 行号 1 起
    }

    private int findMemberEnd(String[] lines, int declIdx) {
        String declLine = lines[declIdx];
        if (declLine.contains("{")) {               // TS/JS: 花括号配平
            int depth = 0;
            for (int i = declIdx; i < lines.length; i++) {
                for (char c : lines[i].toCharArray()) {
                    if (c == '{') depth++;
                    else if (c == '}') depth--;
                }
                if (depth <= 0 && i > declIdx - 1) return i;
            }
            return lines.length - 1;
        }
        int baseIndent = indentOf(declLine);        // Python: 缩进回落
        int last = declIdx;
        for (int i = declIdx + 1; i < lines.length; i++) {
            if (lines[i].isBlank()) continue;
            if (indentOf(lines[i]) <= baseIndent) break;
            last = i;
        }
        return last;
    }

    private int indentOf(String s) {
        int n = 0;
        while (n < s.length() && (s.charAt(n) == ' ' || s.charAt(n) == '\t')) n++;
        return n;
    }

    private String md5Hex(String s) {
        try {
            byte[] d = java.security.MessageDigest.getInstance("MD5")
                .digest(s == null ? new byte[0] : s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeRel(String p) { return String.valueOf(p).replace('\\', '/'); }

    private void writeFileRaw(String relPath, String content) throws IOException {
        Path p = safeResolve(relPath);
        Files.createDirectories(p.getParent());
        Files.writeString(p, content == null ? "" : content, StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    /** 路径穿越防护:任何解析结果必须落在工作区内 */
    private Path safeResolve(String relPath) {
        if (relPath == null || relPath.isBlank()) throw new IllegalArgumentException("路径不能为空");
        Path p = root.resolve(relPath.replace('\\', '/')).normalize();
        if (!p.startsWith(root)) throw new IllegalArgumentException("非法路径(越出工作区): " + relPath);
        return p;
    }

    /* ==================== 状态 / 历史 ==================== */

    public Map<String, Object> status() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", enabled);
        out.put("ready", ready);
        out.put("error", lastError);
        out.put("workdir", root == null ? null : root.toString());
        out.put("branch", branch);
        out.put("remote", maskUrl(effectiveUrl()));
        // 远程是从环境变量来的还是从工作区 origin 兜底来的, 前端据此提示
        out.put("remote_source", (url != null && !url.isBlank()) ? "config"
            : (workdirRemote != null && !workdirRemote.isBlank()) ? "workdir" : null);
        out.put("has_credentials", token != null && !token.isBlank());
        out.put("auto_push", autoPush);
        if (!ready) return out;
        try (Git git = open()) {
            Repository repo = git.getRepository();
            out.put("current_branch", repo.getBranch());
            ObjectId head = repo.resolve(Constants.HEAD);
            out.put("head", head == null ? null : head.getName());
            out.put("dirty", !git.status().call().isClean());
            out.put("ahead", aheadCount(repo));   // 待推送提交数, IDE 版本变更面板的角标
        } catch (Exception e) {
            out.put("error", e.getMessage());
        }
        return out;
    }

    public List<Map<String, Object>> history(String relPath, int limit) throws Exception {
        requireReady();
        List<Map<String, Object>> out = new ArrayList<>();
        try (Git git = open()) {
            if (!hasCommits(git)) return out;
            var cmd = git.log().setMaxCount(Math.max(1, limit));
            if (relPath != null && !relPath.isBlank()) cmd.addPath(relPath.replace('\\', '/'));
            for (RevCommit c : cmd.call()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("commit", c.getName());
                row.put("short", c.getName().substring(0, 8));
                row.put("message", c.getShortMessage());
                row.put("author", c.getAuthorIdent().getName());
                row.put("time", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.ofEpochSecond(c.getCommitTime())));
                out.add(row);
            }
        }
        return out;
    }

    /* ==================== 提交对比 ==================== */

    /** 单个文件对比的内容上限;超过就只给统计不给正文,免得把几 MB 的文本推给浏览器 */
    private static final int DIFF_MAX_BYTES = 512 * 1024;

    /**
     * 一次提交的详情:元信息 + 变更文件清单(含增删行数)。
     * 首次提交没有父提交,与空树比。
     */
    public Map<String, Object> commitDetail(String sha) throws Exception {
        requireReady();
        try (Git git = open()) {
            Repository repo = git.getRepository();
            ObjectId id = repo.resolve(sha);
            if (id == null) throw new IllegalArgumentException("提交不存在: " + sha);
            try (RevWalk rw = new RevWalk(repo)) {
                RevCommit c = rw.parseCommit(id);
                RevCommit parent = c.getParentCount() > 0 ? rw.parseCommit(c.getParent(0).getId()) : null;

                Map<String, Object> out = new LinkedHashMap<>();
                out.put("commit", c.getName());
                out.put("short", c.getName().substring(0, 8));
                out.put("parent", parent == null ? null : parent.getName());
                out.put("message", c.getFullMessage());
                out.put("author", c.getAuthorIdent().getName());
                out.put("email", c.getAuthorIdent().getEmailAddress());
                out.put("time", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.ofEpochSecond(c.getCommitTime())));
                out.put("files", diffFiles(repo, parent, c));
                return out;
            }
        }
    }

    private List<Map<String, Object>> diffFiles(Repository repo, RevCommit parent, RevCommit c) throws IOException {
        List<Map<String, Object>> files = new ArrayList<>();
        try (ObjectReader reader = repo.newObjectReader();
             DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
            df.setRepository(repo);
            df.setDetectRenames(true);
            AbstractTreeIterator oldTree = parent == null ? new EmptyTreeIterator() : treeIter(reader, parent);
            AbstractTreeIterator newTree = treeIter(reader, c);
            for (DiffEntry d : df.scan(oldTree, newTree)) {
                Map<String, Object> f = new LinkedHashMap<>();
                String oldPath = DiffEntry.DEV_NULL.equals(d.getOldPath()) ? null : d.getOldPath();
                String newPath = DiffEntry.DEV_NULL.equals(d.getNewPath()) ? null : d.getNewPath();
                f.put("change", d.getChangeType().name());
                f.put("old_path", oldPath);
                f.put("new_path", newPath);
                f.put("path", newPath != null ? newPath : oldPath);
                int add = 0, del = 0;
                try {
                    for (var e : df.toFileHeader(d).toEditList()) {
                        add += e.getEndB() - e.getBeginB();
                        del += e.getEndA() - e.getBeginA();
                    }
                } catch (Exception ignore) {
                    // 二进制文件算不出行数,留 0
                }
                f.put("additions", add);
                f.put("deletions", del);
                files.add(f);
            }
        }
        return files;
    }

    private AbstractTreeIterator treeIter(ObjectReader reader, RevCommit c) throws IOException {
        CanonicalTreeParser p = new CanonicalTreeParser();
        p.reset(reader, c.getTree().getId());
        return p;
    }

    /**
     * 取某个文件在一次提交前后的两份正文,供前端 diff 编辑器左右两栏使用。
     * 新增文件的 old 为空,删除文件的 new 为空。
     */
    public Map<String, Object> commitFile(String sha, String relPath) throws Exception {
        requireReady();
        String path = relPath == null ? "" : relPath.replace('\\', '/');
        if (path.isBlank()) throw new IllegalArgumentException("路径不能为空");
        try (Git git = open()) {
            Repository repo = git.getRepository();
            ObjectId id = repo.resolve(sha);
            if (id == null) throw new IllegalArgumentException("提交不存在: " + sha);
            try (RevWalk rw = new RevWalk(repo)) {
                RevCommit c = rw.parseCommit(id);
                RevCommit parent = c.getParentCount() > 0 ? rw.parseCommit(c.getParent(0).getId()) : null;
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("path", path);
                out.put("commit", c.getName());
                Map<String, Object> nw = blobAt(repo, c, path);
                Map<String, Object> od = parent == null
                    ? Map.of("content", "", "exists", false) : blobAt(repo, parent, path);
                out.put("new_content", nw.get("content"));
                out.put("old_content", od.get("content"));
                out.put("new_exists", nw.get("exists"));
                out.put("old_exists", od.get("exists"));
                out.put("binary", Boolean.TRUE.equals(nw.get("binary")) || Boolean.TRUE.equals(od.get("binary")));
                out.put("too_large", Boolean.TRUE.equals(nw.get("too_large")) || Boolean.TRUE.equals(od.get("too_large")));
                return out;
            }
        }
    }

    private Map<String, Object> blobAt(Repository repo, RevCommit c, String path) throws IOException {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("content", "");
        out.put("exists", false);
        try (TreeWalk tw = TreeWalk.forPath(repo, path, c.getTree())) {
            if (tw == null) return out;
            out.put("exists", true);
            ObjectLoader loader = repo.open(tw.getObjectId(0));
            if (loader.getSize() > DIFF_MAX_BYTES) {
                out.put("too_large", true);
                return out;
            }
            byte[] bytes = loader.getBytes();
            if (RawText.isBinary(bytes)) {
                out.put("binary", true);
                return out;
            }
            out.put("content", new String(bytes, StandardCharsets.UTF_8));
        }
        return out;
    }

    /* ==================== 分支管理 ==================== */

    /** 本地分支列表(标出当前分支与是否有对应远程分支) */
    public List<Map<String, Object>> branches() throws Exception {
        requireReady();
        List<Map<String, Object>> out = new ArrayList<>();
        try (Git git = open()) {
            String cur = git.getRepository().getBranch();
            Set<String> remotes = new HashSet<>();
            for (var ref : git.branchList().setListMode(org.eclipse.jgit.api.ListBranchCommand.ListMode.REMOTE).call()) {
                String n = ref.getName();                       // refs/remotes/origin/xxx
                int i = n.indexOf("/origin/");
                if (i >= 0) remotes.add(n.substring(i + "/origin/".length()));
            }
            for (var ref : git.branchList().call()) {
                String name = ref.getName().replaceFirst("^refs/heads/", "");
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("name", name);
                row.put("current", name.equals(cur));
                row.put("has_remote", remotes.contains(name));
                row.put("head", ref.getObjectId() == null ? null : ref.getObjectId().getName().substring(0, 8));
                // 相对远程同名分支的领先/落后(没有远程分支时为 null, 前端不显示)
                int[] ab = remotes.contains(name)
                    ? countAheadBehind(git.getRepository(), "refs/heads/" + name,
                                       "refs/remotes/origin/" + name)
                    : null;
                row.put("ahead", ab == null ? null : ab[0]);
                row.put("behind", ab == null ? null : ab[1]);
                // 相对当前分支多出的提交数 —— 决定"要不要合并过来"时看这个
                row.put("unmerged", name.equals(cur) ? 0
                    : countAheadBehind(git.getRepository(), "refs/heads/" + name, Constants.HEAD)[0]);
                out.add(row);
            }
            // 远程独有的分支也列出来, 方便切过去(检出时自动建本地跟踪分支)
            Set<String> localNames = out.stream().map(r -> str(r.get("name"))).collect(Collectors.toSet());
            for (String r : remotes) {
                if (localNames.contains(r)) continue;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("name", r);
                row.put("current", false);
                row.put("has_remote", true);
                row.put("remote_only", true);
                out.add(row);
            }
        }
        return out;
    }

    /**
     * a 相对 b 的 [领先, 落后] 提交数。
     * 任一端解析不到就返回 [0, 0] —— 界面上宁可不显示, 也不要显示一个错的数字。
     */
    private int[] countAheadBehind(Repository repo, String a, String b) {
        try (RevWalk walk = new RevWalk(repo)) {
            ObjectId ida = repo.resolve(a), idb = repo.resolve(b);
            if (ida == null || idb == null) return new int[]{0, 0};
            RevCommit ca = walk.parseCommit(ida), cb = walk.parseCommit(idb);
            return new int[]{ countRange(repo, ca, cb), countRange(repo, cb, ca) };
        } catch (Exception e) {
            log.debug("[fn-repo] 计算领先/落后失败: {}", e.getMessage());
            return new int[]{0, 0};
        }
    }

    /** from 有而 to 没有的提交数 */
    private int countRange(Repository repo, RevCommit from, RevCommit to) throws IOException {
        try (RevWalk walk = new RevWalk(repo)) {
            walk.markStart(walk.parseCommit(from.getId()));
            walk.markUninteresting(walk.parseCommit(to.getId()));
            int n = 0;
            for (RevCommit ignored : walk) n++;
            return n;
        }
    }

    /**
     * 把指定分支合并进当前分支。
     * <p>
     * 冲突时**不留半合并状态**:直接回滚到合并前的 HEAD, 把冲突文件清单返回给前端。
     * IDE 目前没有冲突解决界面, 留一堆带冲突标记的文件在工作区只会让人更难收场。
     */
    public synchronized Map<String, Object> mergeBranch(String name, boolean noFastForward) throws Exception {
        requireReady();
        String n = normalizeBranch(name);
        try (Git git = open()) {
            String cur = currentBranch(git.getRepository());
            if (n.equals(cur)) throw new IllegalStateException("不能把分支合并到它自己");
            if (!git.status().call().isClean())
                throw new IllegalStateException("工作区有未提交的改动, 请先保存(提交)或撤销后再合并");
            var ref = git.getRepository().findRef(n);
            if (ref == null) throw new IllegalArgumentException("分支不存在: " + n);

            ObjectId before = git.getRepository().resolve(Constants.HEAD);
            var res = git.merge()
                .include(ref)
                .setFastForward(noFastForward
                    ? org.eclipse.jgit.api.MergeCommand.FastForwardMode.NO_FF
                    : org.eclipse.jgit.api.MergeCommand.FastForwardMode.FF)
                .setCommit(true)
                .setMessage("merge: 合并 " + n + " 到 " + cur + " (BOntoLink IDE)")
                .call();

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("status", res.getMergeStatus().name());
            out.put("into", cur);
            out.put("from", n);
            if (res.getMergeStatus() == org.eclipse.jgit.api.MergeResult.MergeStatus.CONFLICTING) {
                List<String> conflicts = res.getConflicts() == null
                    ? List.of() : new ArrayList<>(res.getConflicts().keySet());
                git.reset().setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD)
                   .setRef(before.getName()).call();
                out.put("ok", false);
                out.put("conflicts", conflicts);
                out.put("message", "存在冲突, 已回滚到合并前状态。冲突文件: " + String.join(", ", conflicts));
                return out;
            }
            out.put("ok", res.getMergeStatus().isSuccessful());
            ObjectId head = git.getRepository().resolve(Constants.HEAD);
            out.put("head", head == null ? null : head.getName());
            if (!res.getMergeStatus().isSuccessful())
                out.put("message", "合并未成功: " + res.getMergeStatus());
            return out;
        }
    }

    /* ==================== 未提交改动 ==================== */

    /** 工作区未提交的改动清单(供「撤销」用) */
    public Map<String, Object> changes() throws Exception {
        requireReady();
        try (Git git = open()) {
            var st = git.status().call();
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("modified", sorted(st.getModified(), st.getChanged()));
            out.put("added", sorted(st.getAdded()));
            out.put("removed", sorted(st.getRemoved(), st.getMissing()));
            out.put("untracked", sorted(st.getUntracked()));
            out.put("conflicting", sorted(st.getConflicting()));
            out.put("clean", st.isClean());
            return out;
        }
    }

    @SafeVarargs
    private List<String> sorted(Set<String>... sets) {
        Set<String> all = new TreeSet<>();
        for (Set<String> s : sets) if (s != null) all.addAll(s);
        return new ArrayList<>(all);
    }

    /**
     * 撤销未提交的改动:回到 HEAD 的状态。
     * path 为空 = 全部撤销(含删除新增的未跟踪文件);否则只撤这一个文件。
     */
    public synchronized Map<String, Object> discard(String relPath) throws Exception {
        requireReady();
        Map<String, Object> out = new LinkedHashMap<>();
        try (Git git = open()) {
            if (relPath == null || relPath.isBlank()) {
                git.reset().setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD).call();
                Set<String> removed = git.clean().setCleanDirectories(true).call();
                out.put("scope", "all");
                out.put("removed_untracked", new ArrayList<>(removed));
            } else {
                String path = relPath.replace('\\', '/');
                safeResolve(path);                       // 路径穿越防护
                boolean tracked = git.status().call().getUntracked().stream().noneMatch(path::equals);
                if (tracked) {
                    git.checkout().addPath(path).call();
                } else {
                    // 未跟踪文件没有"上一版"可回滚, 撤销即删除
                    Files.deleteIfExists(safeResolve(path));
                }
                out.put("scope", "file");
                out.put("path", path);
                out.put("was_tracked", tracked);
            }
        }
        return out;
    }

    /**
     * 删除本地分支。
     * 当前分支不能删(git 本身也不允许);有未合并提交时不带 force 会被 JGit 拒绝,
     * 前端据此二次确认后再带 force 重试 —— 不做静默强删。
     */
    public synchronized Map<String, Object> deleteBranch(String name, boolean force) throws Exception {
        requireReady();
        String n = normalizeBranch(name);
        try (Git git = open()) {
            if (n.equals(git.getRepository().getBranch()))
                throw new IllegalStateException("不能删除当前所在分支, 请先切到其他分支");
            boolean exists = git.branchList().call().stream()
                .anyMatch(r -> r.getName().equals("refs/heads/" + n));
            if (!exists) throw new IllegalArgumentException("本地不存在分支: " + n);
            try {
                List<String> deleted = git.branchDelete()
                    .setBranchNames(n).setForce(force).call();
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("deleted", deleted);
                out.put("forced", force);
                return out;
            } catch (org.eclipse.jgit.api.errors.NotMergedException e) {
                throw new IllegalStateException("分支 " + n + " 有未合并的提交, 删除会丢失这些提交");
            }
        }
    }

    /** 新建分支(基于当前 HEAD),可选立即切过去 */
    public synchronized Map<String, Object> createBranch(String name, boolean checkout) throws Exception {
        requireReady();
        String n = normalizeBranch(name);
        try (Git git = open()) {
            if (!hasCommits(git)) throw new IllegalStateException("仓库还没有任何提交, 无法建分支");
            git.branchCreate().setName(n).call();
            if (checkout) git.checkout().setName(n).call();
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("name", n);
            out.put("checked_out", checkout);
            return out;
        }
    }

    /**
     * 切换分支。
     * 工作区有未提交改动时直接拒绝 —— 宁可让用户先保存/丢弃, 也不做自动 stash 这种"帮倒忙"的事。
     */
    public synchronized Map<String, Object> checkoutBranch(String name) throws Exception {
        requireReady();
        String n = normalizeBranch(name);
        try (Git git = open()) {
            if (!git.status().call().isClean())
                throw new IllegalStateException("工作区有未提交的改动, 请先保存(提交)后再切换分支");
            boolean localExists = git.getRepository().resolve("refs/heads/" + n) != null;
            var cmd = git.checkout().setName(n);
            if (!localExists) {
                // 远程独有的分支:建本地跟踪分支
                cmd.setCreateBranch(true)
                   .setStartPoint(Constants.DEFAULT_REMOTE_NAME + "/" + n)
                   .setUpstreamMode(org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode.TRACK);
            }
            cmd.call();
            branch = n;      // 后续提交 / 推送跟随当前分支
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("name", n);
            out.put("switched", true);
            return out;
        }
    }

    /** 分支名基本校验:不接受空格与 git 保留字符 */
    private String normalizeBranch(String name) {
        String n = name == null ? "" : name.trim();
        if (n.isEmpty()) throw new IllegalArgumentException("分支名不能为空");
        if (!n.matches("^[A-Za-z0-9._/-]+$"))
            throw new IllegalArgumentException("分支名只能包含字母、数字、. _ / - ");
        return n;
    }

    /** 手动推送 (auto-push 关闭时给 IDE 用) */
    public synchronized Map<String, Object> pushNow() throws Exception {
        requireReady();
        try (Git git = open()) { return push(git); }
    }

    /* ==================== JGit 细节 ==================== */

    /**
     * best-effort 拉取远程引用。
     * 克隆一个空仓库时不会产生 refs/remotes/origin/*, 之后即使推送过, 「待推送数」也算不出来,
     * 所以初始化阶段补一次 fetch;失败(离线/无权限)只记日志, 不影响 IDE 读写本地工作区。
     */
    private void fetchQuietly() {
        String eff = effectiveUrl();
        if (eff == null || eff.isBlank()) return;
        try (Git git = open()) {
            boolean hasOrigin = git.getRepository().getConfig()
                .getSubsections("remote").contains(Constants.DEFAULT_REMOTE_NAME);
            if (!hasOrigin) return;
            git.fetch()
               .setRemote(Constants.DEFAULT_REMOTE_NAME)
               .setCredentialsProvider(credentials())
               .call();
            log.info("[fn-repo] 已同步远程引用");
        } catch (Exception e) {
            log.warn("[fn-repo] fetch 失败(不影响本地读写): {}", e.getMessage());
        }
    }

    /**
     * 本地领先远程的提交数(= 待推送)。
     * 手动推送模式下 IDE 靠这个值给「推送」按钮打角标;没有远程跟踪分支时返回 0。
     */
    private int aheadCount(Repository repo) {
        try (org.eclipse.jgit.revwalk.RevWalk walk = new org.eclipse.jgit.revwalk.RevWalk(repo)) {
            ObjectId head = repo.resolve(Constants.HEAD);
            // 比的是**当前所在分支**的远程跟踪引用, 不是配置里那个默认分支 ——
            // 切到 feature 分支后再拿 origin/master 比, 数出来的是另一条线的差异
            ObjectId remote = repo.resolve("refs/remotes/origin/" + currentBranch(repo));
            if (head == null) return 0;
            if (remote == null) return -1;               // -1 = 远程还没有该分支(首次推送前)
            walk.markStart(walk.parseCommit(head));
            walk.markUninteresting(walk.parseCommit(remote));
            int n = 0;
            for (RevCommit ignored : walk) n++;
            return n;
        } catch (Exception e) {
            log.debug("[fn-repo] 计算待推送数失败: {}", e.getMessage());
            return 0;
        }
    }

    /** 当前所在分支;detached HEAD 之类拿不到名字时退回配置的默认分支 */
    private String currentBranch(Repository repo) {
        try {
            String b = repo.getBranch();
            return (b == null || b.isBlank()) ? branch : b;
        } catch (Exception e) {
            return branch;
        }
    }

    private Git open() throws IOException {
        Repository repo = new FileRepositoryBuilder()
            .setGitDir(root.resolve(".git").toFile())
            .readEnvironment()
            .build();
        return new Git(repo);
    }

    private boolean hasCommits(Git git) throws IOException {
        return git.getRepository().resolve(Constants.HEAD) != null;
    }

    /** 克隆下来的空仓库没有 HEAD 分支, 这里保证工作在配置的分支上 */
    private void checkoutBranch(Git git) throws GitAPIException, IOException {
        String cur = git.getRepository().getBranch();
        if (branch.equals(cur)) return;
        boolean exists = git.getRepository().resolve(branch) != null;
        if (exists) git.checkout().setName(branch).call();
        else if (hasCommits(git)) git.checkout().setCreateBranch(true).setName(branch).call();
        // 空仓库:JGit 的 HEAD 已指向初始分支, 首次提交后自然落到该分支
    }

    /** add -A + commit;无变更时返回 null */
    private RevCommit commitAll(Git git, String message) throws GitAPIException {
        git.add().addFilepattern(".").call();
        git.add().addFilepattern(".").setUpdate(true).call();     // 捕获删除
        if (git.status().call().isClean()) return null;
        return git.commit()
            .setMessage(message)
            .setAuthor(new PersonIdent(authorName, authorEmail))
            .setCommitter(new PersonIdent(authorName, authorEmail))
            .call();
    }

    private Map<String, Object> push(Git git) {
        Map<String, Object> out = new LinkedHashMap<>();
        String eff = effectiveUrl();
        if (eff == null || eff.isBlank()) {
            out.put("ok", false);
            out.put("message", "未配置远程地址, 跳过推送");
            return out;
        }
        if (token == null || token.isBlank()) {
            out.put("ok", false);
            out.put("message", "远程已配置但缺少访问凭据 (FN_REPO_TOKEN), 无法推送");
            return out;
        }
        try {
            List<String> results = new ArrayList<>();
            // 优先用 origin 这个 remote 名而不是裸 URL:JGit 只有走 remote 名才会同步更新
            // refs/remotes/origin/*, 否则「待推送数」推完仍不归零
            boolean hasOrigin = git.getRepository().getConfig()
                .getSubsections("remote").contains(Constants.DEFAULT_REMOTE_NAME);
            // 推的是当前所在分支, 不是配置里的默认分支 —— 否则在 feature 分支点推送会去推 master
            String cur = currentBranch(git.getRepository());
            var pushResults = git.push()
                .setRemote(hasOrigin ? Constants.DEFAULT_REMOTE_NAME : eff)
                .setRefSpecs(new RefSpec("refs/heads/" + cur + ":refs/heads/" + cur))
                .setCredentialsProvider(credentials())
                .call();
            boolean ok = true;
            for (var pr : pushResults) {
                for (RemoteRefUpdate u : pr.getRemoteUpdates()) {
                    results.add(u.getRemoteName() + " → " + u.getStatus());
                    if (u.getStatus() != RemoteRefUpdate.Status.OK
                        && u.getStatus() != RemoteRefUpdate.Status.UP_TO_DATE) ok = false;
                }
            }
            out.put("ok", ok);
            out.put("details", results);
        } catch (Exception e) {
            out.put("ok", false);
            out.put("message", e.getMessage());
            log.warn("[fn-repo] 推送失败: {}", e.getMessage());
        }
        return out;
    }

    private CredentialsProvider credentials() {
        if (token == null || token.isBlank()) return null;
        String user = (username == null || username.isBlank()) ? "oauth2" : username;
        return new UsernamePasswordCredentialsProvider(user, token);
    }

    private void requireReady() {
        if (!ready) throw new IllegalStateException(
            "函数代码仓不可用" + (lastError == null ? "" : ": " + lastError));
    }

    /** 日志/接口里回显远程地址时抹掉可能内嵌的凭据 */
    private String maskUrl(String u) {
        if (u == null || u.isBlank()) return null;
        return u.replaceAll("://[^@/]+@", "://***@");
    }

    private String nowText() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault()).format(Instant.now());
    }

    /* —— 与 FunctionController 同款的版本比较, 避免跨模块依赖 —— */
    private int compareVersion(String a, String b) {
        String[] xs = a.replaceFirst("^[vV]", "").split("\\.");
        String[] ys = b.replaceFirst("^[vV]", "").split("\\.");
        for (int i = 0; i < Math.max(xs.length, ys.length); i++) {
            int x = i < xs.length ? toInt(xs[i]) : 0;
            int y = i < ys.length ? toInt(ys[i]) : 0;
            if (x != y) return Integer.compare(x, y);
        }
        return 0;
    }
    private String str(Object v) { return v == null ? "" : String.valueOf(v); }
    private int toInt(Object v) {
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(String.valueOf(v).trim()); } catch (Exception e) { return 0; }
    }
}
