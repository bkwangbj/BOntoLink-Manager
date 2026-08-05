package com.beiktech.bontolink.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 函数断点调试服务 (P8)
 * <p>
 * <b>设计要点:Java 只做透明管道, 不解析 DAP 语义。</b>
 * 调试器(Python 用 debugpy)本身就是标准 DAP 服务端, 前端是 DAP 客户端;
 * 本类负责把 WebSocket 上的 JSON 报文按 DAP 的 {@code Content-Length} 分帧规则
 * 转发给调试器的 TCP 端口, 再把调试器的响应/事件拆帧回推给前端。
 * <p>
 * 这样 initialize / setBreakpoints / stackTrace / variables / evaluate 这些协议细节
 * 全在前端处理 —— UI 本来就要用这些数据, 放在 Java 里再翻译一遍纯属多余, 还容易失真。
 * <p>
 * 语言支持:
 * <ul>
 *   <li>Python:{@code python -m debugpy --listen 127.0.0.1:port --wait-for-client file} —— 原生 DAP,已验证</li>
 *   <li>TypeScript / JavaScript:Node 自带的是 CDP 而非 DAP, 需要额外的 js-debug 适配器;
 *       未安装时明确报错, 不静默失败</li>
 * </ul>
 */
@Service
public class FnDebugService {

    private static final Logger log = LoggerFactory.getLogger(FnDebugService.class);

    @Value("${bontolink.fn-run.enabled:true}")             private boolean enabled;
    @Value("${bontolink.fn-repo.workdir:../fn-repo}")      private String workdir;
    /**
     * -u 关掉标准输出的块缓冲:不加的话被调试程序的 print 要等进程退出才吐出来,
     * 会话被断开杀掉时甚至整段丢失(调试控制台看着像什么都没打)。
     * -X utf8 保证中文输出不被 Windows 的 GBK 控制台编码弄乱。
     */
    @Value("${bontolink.fn-debug.python-cmd:python -X utf8 -u}") private String pythonCmd;
    /** js-debug 适配器启动命令;为空表示未部署, TS/JS 调试直接报错 */
    @Value("${bontolink.fn-debug.node-adapter-cmd:}")      private String nodeAdapterCmd;
    @Value("${bontolink.fn-debug.connect-timeout-ms:15000}") private int connectTimeoutMs;

    private final Map<String, DebugSession> sessions = new ConcurrentHashMap<>();

    private static class DebugSession {
        Process process;
        Socket socket;
        Thread readerThread, stdoutThread, stderrThread;
        volatile boolean closing = false;
    }

    /* ==================== 生命周期 ==================== */

    /** 启动调试:拉起带调试器的进程, 连上它的 DAP 端口, 之后前端与调试器直接对话 */
    public synchronized void start(WebSocketSession ws, String relPath, FnRunService bus) {
        if (!enabled) { bus.event(ws, "debug-error", Map.of("message", "运行/调试能力未启用")); return; }
        if (sessions.containsKey(ws.getId())) {
            bus.event(ws, "debug-error", Map.of("message", "已有调试会话在运行, 请先停止"));
            return;
        }
        Path root = Paths.get(workdir).toAbsolutePath().normalize();
        Path file;
        try {
            file = root.resolve(String.valueOf(relPath).replace('\\', '/')).normalize();
            if (!file.startsWith(root)) throw new IllegalArgumentException("非法路径(越出工作区): " + relPath);
        } catch (Exception e) {
            bus.event(ws, "debug-error", Map.of("message", e.getMessage()));
            return;
        }
        if (!file.toFile().isFile()) {
            bus.event(ws, "debug-error", Map.of("message", "文件不存在: " + relPath));
            return;
        }

        String lower = String.valueOf(relPath).toLowerCase();
        if (!lower.endsWith(".py")) {
            if (nodeAdapterCmd == null || nodeAdapterCmd.isBlank()) {
                bus.event(ws, "debug-error", Map.of("message",
                    "当前只支持 Python 断点调试。TypeScript / JavaScript 需要 js-debug 适配器 —— " +
                    "Node 原生走的是 CDP 而非 DAP,请先部署适配器并配置 bontolink.fn-debug.node-adapter-cmd"));
                return;
            }
        }

        int port = freePort();
        if (port < 0) { bus.event(ws, "debug-error", Map.of("message", "没有可用的调试端口")); return; }

        DebugSession ds = new DebugSession();
        try {
            String cmd = pythonCmd + " -m debugpy --listen 127.0.0.1:" + port
                       + " --wait-for-client \"" + file.toAbsolutePath() + "\"";
            ProcessBuilder pb = new ProcessBuilder(shellWrap(cmd));
            pb.directory(root.toFile());
            pb.environment().put("PYTHONIOENCODING", "utf-8");
            pb.environment().put("PYTHONUTF8", "1");
            ds.process = pb.start();
            bus.event(ws, "debug-started", Map.of("path", relPath, "port", port, "command", cmd));

            // 被调试进程自己的 stdout/stderr 也要能看到(调试控制台里显示)
            ds.stdoutThread = pumpProcess(ws, bus, ds.process.getInputStream(), "stdout");
            ds.stderrThread = pumpProcess(ws, bus, ds.process.getErrorStream(), "stderr");

            ds.socket = connectWithRetry(port);
            if (ds.socket == null) {
                bus.event(ws, "debug-error", Map.of("message", "调试器端口连接超时(" + connectTimeoutMs + "ms)"));
                killProcess(ds);
                return;
            }
            sessions.put(ws.getId(), ds);
            ds.readerThread = startDapReader(ws, bus, ds);
            bus.event(ws, "debug-attached", Map.of("port", port));
        } catch (Exception e) {
            log.warn("[fn-debug] 启动失败: {}", e.getMessage());
            bus.event(ws, "debug-error", Map.of("message", "启动调试失败: " + e.getMessage()));
            killProcess(ds);
        }
    }

    /** 前端 → 调试器:按 DAP 分帧写入 */
    public void forward(WebSocketSession ws, JSONObject dapMessage, FnRunService bus) {
        DebugSession ds = sessions.get(ws.getId());
        if (ds == null || ds.socket == null) {
            bus.event(ws, "debug-error", Map.of("message", "没有活动的调试会话"));
            return;
        }
        byte[] body = JSON.toJSONString(dapMessage).getBytes(StandardCharsets.UTF_8);
        try {
            OutputStream out = ds.socket.getOutputStream();
            synchronized (ds) {
                out.write(("Content-Length: " + body.length + "\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                out.write(body);
                out.flush();
            }
        } catch (IOException e) {
            bus.event(ws, "debug-error", Map.of("message", "写调试器失败: " + e.getMessage()));
        }
    }

    /** 停止调试:关 socket + 杀进程 */
    public synchronized void stop(WebSocketSession ws, FnRunService bus) {
        DebugSession ds = sessions.remove(ws.getId());
        if (ds == null) {
            if (bus != null) bus.event(ws, "debug-error", Map.of("message", "当前没有调试会话"));
            return;
        }
        ds.closing = true;
        closeQuietly(ds.socket);
        killProcess(ds);
        if (bus != null) bus.event(ws, "debug-terminated", Map.of("reason", "stopped"));
    }

    public void release(String sessionId) {
        DebugSession ds = sessions.remove(sessionId);
        if (ds == null) return;
        ds.closing = true;
        closeQuietly(ds.socket);
        killProcess(ds);
    }

    public boolean isDebugging(String sessionId) { return sessions.containsKey(sessionId); }

    public Map<String, Object> capabilities() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("python_debug", true);
        out.put("node_debug", nodeAdapterCmd != null && !nodeAdapterCmd.isBlank());
        out.put("python_cmd", pythonCmd);
        return out;
    }

    /* ==================== 内部 ==================== */

    /** 调试器 → 前端:按 Content-Length 拆帧, 原样转发 DAP 报文 */
    private Thread startDapReader(WebSocketSession ws, FnRunService bus, DebugSession ds) {
        Thread t = new Thread(() -> {
            try (InputStream in = ds.socket.getInputStream()) {
                while (!ds.closing) {
                    int len = readContentLength(in);
                    if (len < 0) break;
                    byte[] body = in.readNBytes(len);
                    if (body.length < len) break;
                    String json = new String(body, StandardCharsets.UTF_8);
                    bus.event(ws, "dap", Map.of("message", JSON.parseObject(json)));
                }
            } catch (Exception e) {
                if (!ds.closing) log.debug("[fn-debug] DAP 读取结束: {}", e.getMessage());
            } finally {
                if (!ds.closing) {
                    sessions.remove(ws.getId());
                    bus.event(ws, "debug-terminated", Map.of("reason", "disconnected"));
                }
            }
        }, "fn-dap-reader");
        t.setDaemon(true);
        t.start();
        return t;
    }

    /** 读 DAP 头部, 返回正文长度;流结束返回 -1 */
    private int readContentLength(InputStream in) throws IOException {
        StringBuilder header = new StringBuilder();
        int len = -1;
        while (true) {
            int b = in.read();
            if (b < 0) return -1;
            header.append((char) b);
            if (header.length() >= 4 && header.substring(header.length() - 4).equals("\r\n\r\n")) break;
            if (header.length() > 8192) throw new IOException("DAP 头部异常过长");
        }
        for (String line : header.toString().split("\r\n")) {
            String l = line.trim();
            if (l.toLowerCase().startsWith("content-length:")) {
                len = Integer.parseInt(l.substring(l.indexOf(':') + 1).trim());
            }
        }
        if (len < 0) throw new IOException("DAP 头部缺少 Content-Length");
        return len;
    }

    /** debugpy 启动要一点时间, 端口没起来就重试到超时 */
    private Socket connectWithRetry(int port) {
        long deadline = System.currentTimeMillis() + connectTimeoutMs;
        while (System.currentTimeMillis() < deadline) {
            try {
                Socket s = new Socket();
                s.connect(new InetSocketAddress("127.0.0.1", port), 1000);
                s.setTcpNoDelay(true);
                return s;
            } catch (IOException ignored) {
                try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return null; }
            }
        }
        return null;
    }

    /** 被调试进程的控制台输出 */
    private Thread pumpProcess(WebSocketSession ws, FnRunService bus, InputStream in, String stream) {
        Thread t = new Thread(() -> {
            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    bus.event(ws, "output", Map.of("stream", stream, "text", line));
                }
            } catch (IOException ignored) { /* 进程结束属正常 */ }
        }, "fn-debug-" + stream);
        t.setDaemon(true);
        t.start();
        return t;
    }

    private int freePort() {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        } catch (IOException e) {
            return -1;
        }
    }

    private void killProcess(DebugSession ds) {
        Process p = ds == null ? null : ds.process;
        if (p == null || !p.isAlive()) return;
        try {
            if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
                new ProcessBuilder("taskkill", "/PID", String.valueOf(p.pid()), "/T", "/F")
                    .redirectErrorStream(true).start().waitFor(5, TimeUnit.SECONDS);
            } else {
                p.descendants().forEach(ProcessHandle::destroyForcibly);
            }
        } catch (Exception ignored) {
            // 兜底走 destroyForcibly
        } finally {
            p.destroyForcibly();
        }
    }

    private void closeQuietly(Socket s) {
        try { if (s != null) s.close(); } catch (IOException ignored) { }
    }

    private List<String> shellWrap(String cmdLine) {
        return System.getProperty("os.name", "").toLowerCase().contains("win")
            ? List.of("cmd.exe", "/c", cmdLine)
            : List.of("/bin/sh", "-c", cmdLine);
    }
}
