package com.beiktech.bontolink.service;

import com.beiktech.bontolink.data.mapper.FunctionMapper;
import jakarta.annotation.PostConstruct;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
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
                checkoutBranch(git);
            }
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
            if (commit == null) {
                out.put("committed", false);
                out.put("reason", "内容无变化");
                return out;
            }
            out.put("committed", true);
            out.put("commit", commit.getName());
            if (autoPush) out.put("push", push(git));
        }
        return out;
    }

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
        out.put("remote", maskUrl(url));
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
        if (url == null || url.isBlank()) return;
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
            ObjectId remote = repo.resolve("refs/remotes/origin/" + branch);
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
        if (url == null || url.isBlank()) {
            out.put("ok", false);
            out.put("message", "未配置远程地址, 跳过推送");
            return out;
        }
        try {
            List<String> results = new ArrayList<>();
            // 优先用 origin 这个 remote 名而不是裸 URL:JGit 只有走 remote 名才会同步更新
            // refs/remotes/origin/*, 否则「待推送数」推完仍不归零
            boolean hasOrigin = git.getRepository().getConfig()
                .getSubsections("remote").contains(Constants.DEFAULT_REMOTE_NAME);
            var pushResults = git.push()
                .setRemote(hasOrigin ? Constants.DEFAULT_REMOTE_NAME : url)
                .setRefSpecs(new RefSpec("refs/heads/" + branch + ":refs/heads/" + branch))
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
