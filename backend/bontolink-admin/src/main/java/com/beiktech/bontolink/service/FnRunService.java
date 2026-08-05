package com.beiktech.bontolink.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 函数运行服务 (P7 · 非调试模式)
 * <p>
 * 一个 WebSocket 会话对应至多一个运行中的进程(文档「单会话单连接」)。
 * 进程在代码仓工作区内启动, stdout / stderr 分流逐行推给前端, 支持停止与超时强杀。
 * <p>
 * <b>断点调试不在这里</b>:DAP 调试内核是 P8 的事, 本类只做「以非调试模式运行」与终端命令。
 * <p>
 * <b>安全边界</b>:终端能力等于在服务器上执行任意命令, 因此
 * ① 由 bontolink.fn-run.terminal-enabled 单独开关;
 * ② 工作目录锁定在代码仓工作区内;
 * ③ 有超时与输出行数上限, 防止跑飞。
 * 生产环境要按文档换成容器沙箱执行, 当前实现是宿主进程直跑, 只适合内网开发环境。
 */
@Service
public class FnRunService {

    private static final Logger log = LoggerFactory.getLogger(FnRunService.class);

    @Value("${bontolink.fn-run.enabled:true}")            private boolean enabled;
    @Value("${bontolink.fn-run.terminal-enabled:true}")   private boolean terminalEnabled;
    @Value("${bontolink.fn-run.timeout-seconds:600}")     private int timeoutSeconds;
    @Value("${bontolink.fn-run.max-output-lines:5000}")   private int maxOutputLines;
    /** -u 关掉块缓冲, 让输出边跑边出现在输出面板;不加的话长任务要等结束才一次性刷出来 */
    @Value("${bontolink.fn-run.python-cmd:python -X utf8 -u}") private String pythonCmd;
    @Value("${bontolink.fn-run.node-cmd:node}")           private String nodeCmd;
    @Value("${bontolink.fn-run.ts-cmd:npx --yes tsx}")    private String tsCmd;
    @Value("${bontolink.fn-repo.workdir:../fn-repo}")     private String workdir;

    /** sessionId → 正在跑的进程 */
    private final Map<String, Proc> running = new ConcurrentHashMap<>();

    private static class Proc {
        Process process;
        Thread outThread, errThread, guardThread;
        volatile boolean stopping = false;
        final AtomicInteger lines = new AtomicInteger();
    }

    /* ==================== 对外入口 ==================== */

    /** 以非调试模式运行某个文件 */
    public void run(WebSocketSession session, String relPath) {
        if (!enabled) { event(session, "error", Map.of("message", "运行能力未启用")); return; }
        if (running.containsKey(session.getId())) {
            event(session, "error", Map.of("message", "已有进程在运行, 请先停止"));
            return;
        }
        Path root = repoRoot();
        Path file;
        try {
            file = safeResolve(root, relPath);
        } catch (Exception e) {
            event(session, "error", Map.of("message", e.getMessage()));
            return;
        }
        if (!file.toFile().isFile()) {
            event(session, "error", Map.of("message", "文件不存在: " + relPath));
            return;
        }
        String cmdLine = commandFor(relPath, file);
        if (cmdLine == null) {
            event(session, "error", Map.of("message", "不支持直接运行该类型的文件: " + relPath));
            return;
        }
        spawn(session, cmdLine, root, "run", relPath);
    }

    /** 终端里执行一条命令 */
    public void terminal(WebSocketSession session, String command) {
        if (!terminalEnabled) { event(session, "error", Map.of("message", "终端能力未启用")); return; }
        if (command == null || command.isBlank()) return;
        if (running.containsKey(session.getId())) {
            event(session, "error", Map.of("message", "已有进程在运行, 请先停止 (Ctrl+C)"));
            return;
        }
        spawn(session, command, repoRoot(), "terminal", command);
    }

    /** 停止当前会话的进程 */
    public void stop(WebSocketSession session) {
        Proc p = running.get(session.getId());
        if (p == null) { event(session, "error", Map.of("message", "当前没有正在运行的进程")); return; }
        p.stopping = true;
        killTree(p.process);
        event(session, "output", Map.of("stream", "system", "text", "^C 已请求停止进程"));
    }

    /** 会话断开:清掉它的进程, 不留孤儿 */
    public void release(String sessionId) {
        Proc p = running.remove(sessionId);
        if (p != null) {
            p.stopping = true;
            killTree(p.process);
        }
    }

    public Map<String, Object> capabilities() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", enabled);
        out.put("terminal_enabled", terminalEnabled);
        out.put("timeout_seconds", timeoutSeconds);
        out.put("python_cmd", pythonCmd);
        out.put("node_cmd", nodeCmd);
        out.put("ts_cmd", tsCmd);
        out.put("workdir", repoRoot().toString());
        return out;
    }

    /* ==================== 进程 ==================== */

    private void spawn(WebSocketSession session, String cmdLine, Path cwd, String kind, String label) {
        ProcessBuilder pb = new ProcessBuilder(shellWrap(cmdLine));
        pb.directory(cwd.toFile());
        // 让子进程按 UTF-8 输出, 避免 Windows 下中文乱码
        pb.environment().put("PYTHONIOENCODING", "utf-8");
        pb.environment().put("PYTHONUTF8", "1");

        Proc proc = new Proc();
        try {
            proc.process = pb.start();
        } catch (IOException e) {
            event(session, "error", Map.of("message", "启动失败: " + e.getMessage()));
            return;
        }
        running.put(session.getId(), proc);
        event(session, "started", Map.of("kind", kind, "label", label, "command", cmdLine, "cwd", cwd.toString()));

        proc.outThread = pump(session, proc, proc.process.getInputStream(), "stdout");
        proc.errThread = pump(session, proc, proc.process.getErrorStream(), "stderr");

        proc.guardThread = new Thread(() -> {
            int code = -1;
            boolean timeout = false;
            try {
                if (!proc.process.waitFor(Math.max(1, timeoutSeconds), TimeUnit.SECONDS)) {
                    timeout = true;
                    killTree(proc.process);
                }
                code = proc.process.waitFor();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } finally {
                running.remove(session.getId());
                joinQuietly(proc.outThread);
                joinQuietly(proc.errThread);
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("code", code);
                body.put("kind", kind);
                body.put("timeout", timeout);
                body.put("stopped", proc.stopping);
                event(session, "exit", body);
            }
        }, "fn-run-guard-" + session.getId());
        proc.guardThread.setDaemon(true);
        proc.guardThread.start();
    }

    /** 逐行读流并推给前端;超过上限就截断, 免得几十万行日志把浏览器打死 */
    private Thread pump(WebSocketSession session, Proc proc, java.io.InputStream in, String stream) {
        Thread t = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    int n = proc.lines.incrementAndGet();
                    if (n == maxOutputLines + 1) {
                        event(session, "output", Map.of("stream", "system",
                            "text", "…输出超过 " + maxOutputLines + " 行, 后续内容已省略"));
                        continue;
                    }
                    if (n > maxOutputLines) continue;
                    event(session, "output", Map.of("stream", stream, "text", line));
                }
            } catch (IOException ignored) {
                // 进程被杀时流会中断, 属正常路径
            }
        }, "fn-run-" + stream + "-" + session.getId());
        t.setDaemon(true);
        t.start();
        return t;
    }

    /** Windows 用 taskkill 杀整棵进程树, 否则 npx/python 起的孙子进程会变孤儿 */
    private void killTree(Process p) {
        if (p == null || !p.isAlive()) return;
        try {
            if (isWindows()) {
                new ProcessBuilder("taskkill", "/PID", String.valueOf(p.pid()), "/T", "/F")
                    .redirectErrorStream(true).start().waitFor(5, TimeUnit.SECONDS);
            } else {
                p.descendants().forEach(ProcessHandle::destroyForcibly);
            }
        } catch (Exception e) {
            log.debug("[fn-run] taskkill 失败, 退回 destroyForcibly: {}", e.getMessage());
        } finally {
            p.destroyForcibly();
        }
    }

    /* ==================== 工具 ==================== */

    /** 按后缀选运行命令;命令模板可通过配置覆盖 */
    private String commandFor(String relPath, Path file) {
        String p = relPath.toLowerCase();
        String quoted = "\"" + file.toAbsolutePath() + "\"";
        if (p.endsWith(".py")) return pythonCmd + " " + quoted;
        if (p.endsWith(".js") || p.endsWith(".mjs") || p.endsWith(".cjs")) return nodeCmd + " " + quoted;
        if (p.endsWith(".ts")) return tsCmd + " " + quoted;
        return null;
    }

    private List<String> shellWrap(String cmdLine) {
        return isWindows()
            ? List.of("cmd.exe", "/c", cmdLine)
            : List.of("/bin/sh", "-c", cmdLine);
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private Path repoRoot() {
        return Paths.get(workdir).toAbsolutePath().normalize();
    }

    /** 与代码仓服务同款的路径穿越防护 */
    private Path safeResolve(Path root, String relPath) {
        if (relPath == null || relPath.isBlank()) throw new IllegalArgumentException("路径不能为空");
        Path p = root.resolve(relPath.replace('\\', '/')).normalize();
        if (!p.startsWith(root)) throw new IllegalArgumentException("非法路径(越出工作区): " + relPath);
        return p;
    }

    private void joinQuietly(Thread t) {
        try { if (t != null) t.join(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    /** 事件下行:{ type:'event', event, body } */
    public void event(WebSocketSession session, String name, Map<String, Object> body) {
        if (session == null || !session.isOpen()) return;
        JSONObject msg = new JSONObject();
        msg.put("type", "event");
        msg.put("event", name);
        msg.put("body", body);
        synchronized (session) {
            try {
                session.sendMessage(new TextMessage(JSON.toJSONString(msg)));
            } catch (Exception e) {
                log.debug("[fn-run] 推送失败: {}", e.getMessage());
            }
        }
    }

    /** 响应下行:{ type:'response', seq, command, success, body } */
    public void response(WebSocketSession session, Object seq, String command, boolean ok, Object body) {
        if (session == null || !session.isOpen()) return;
        JSONObject msg = new JSONObject();
        msg.put("type", "response");
        msg.put("seq", seq);
        msg.put("command", command);
        msg.put("success", ok);
        msg.put("body", body);
        synchronized (session) {
            try {
                session.sendMessage(new TextMessage(JSON.toJSONString(msg)));
            } catch (Exception e) {
                log.debug("[fn-run] 响应失败: {}", e.getMessage());
            }
        }
    }

    public boolean isRunning(String sessionId) { return running.containsKey(sessionId); }
}
