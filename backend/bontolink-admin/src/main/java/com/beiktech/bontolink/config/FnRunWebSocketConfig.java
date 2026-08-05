package com.beiktech.bontolink.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.beiktech.bontolink.service.FnRunService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;

/**
 * 函数 IDE 运行通道的 WebSocket 端点 (P7)
 * <p>
 * 路径: /ws/fn-run(带 context-path 即 /bontolink/ws/fn-run)。
 * 消息格式按文档分三类:
 * <ul>
 *   <li>请求 {@code { type:'request', seq, command, args }} —— run / stop / terminal / ping</li>
 *   <li>响应 {@code { type:'response', seq, command, success, body }}</li>
 *   <li>事件 {@code { type:'event', event, body }} —— started / output / exit / error</li>
 * </ul>
 * 允许跨源握手:开发期前端在 5173,通过 vite 代理连过来。
 */
@Configuration
@EnableWebSocket
public class FnRunWebSocketConfig implements WebSocketConfigurer {

    private static final Logger log = LoggerFactory.getLogger(FnRunWebSocketConfig.class);

    @Autowired private FnRunService runService;
    @Autowired private com.beiktech.bontolink.service.FnDebugService debugService;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new FnRunHandler(runService, debugService), "/ws/fn-run")
                .setAllowedOriginPatterns("*");
    }

    static class FnRunHandler extends TextWebSocketHandler {

        private final FnRunService svc;
        private final com.beiktech.bontolink.service.FnDebugService dbg;

        FnRunHandler(FnRunService svc, com.beiktech.bontolink.service.FnDebugService dbg) {
            this.svc = svc;
            this.dbg = dbg;
        }

        @Override
        public void afterConnectionEstablished(WebSocketSession session) {
            log.info("[fn-run] 会话接入 {}", session.getId());
            Map<String, Object> caps = new java.util.LinkedHashMap<>(svc.capabilities());
            caps.putAll(dbg.capabilities());
            svc.event(session, "ready", caps);
        }

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            JSONObject req;
            try {
                req = JSON.parseObject(message.getPayload());
            } catch (Exception e) {
                svc.event(session, "error", Map.of("message", "报文不是合法 JSON"));
                return;
            }
            Object seq = req.get("seq");
            String command = req.getString("command");
            JSONObject args = req.getJSONObject("args");
            if (command == null) {
                svc.response(session, seq, "", false, Map.of("message", "缺少 command"));
                return;
            }
            try {
                switch (command) {
                    case "run" -> {
                        svc.response(session, seq, command, true, null);
                        svc.run(session, args == null ? null : args.getString("path"));
                    }
                    case "terminal" -> {
                        svc.response(session, seq, command, true, null);
                        svc.terminal(session, args == null ? null : args.getString("command"));
                    }
                    case "stop" -> {
                        svc.response(session, seq, command, true, null);
                        svc.stop(session);
                    }
                    /* —— 断点调试 (P8):Java 只做 DAP 透明管道 —— */
                    case "debug-start" -> {
                        svc.response(session, seq, command, true, null);
                        dbg.start(session, args == null ? null : args.getString("path"), svc);
                    }
                    case "debug-stop" -> {
                        svc.response(session, seq, command, true, null);
                        dbg.stop(session, svc);
                    }
                    case "dap" -> {
                        // 前端发来的原始 DAP 报文, 原样转给调试器, 不做任何语义处理
                        dbg.forward(session, args == null ? null : args.getJSONObject("message"), svc);
                    }
                    case "status" -> svc.response(session, seq, command, true,
                        Map.of("running", svc.isRunning(session.getId()),
                               "debugging", dbg.isDebugging(session.getId())));
                    case "ping" -> svc.response(session, seq, command, true, Map.of("pong", true));
                    default -> svc.response(session, seq, command, false, Map.of("message", "未知命令: " + command));
                }
            } catch (Exception e) {
                log.warn("[fn-run] 处理 {} 失败: {}", command, e.getMessage());
                svc.response(session, seq, command, false, Map.of("message", String.valueOf(e.getMessage())));
            }
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            log.info("[fn-run] 会话断开 {} ({})", session.getId(), status);
            // 连接断了就把它的进程收掉, 不留孤儿(运行进程 + 调试进程都要收)
            svc.release(session.getId());
            dbg.release(session.getId());
        }
    }
}
