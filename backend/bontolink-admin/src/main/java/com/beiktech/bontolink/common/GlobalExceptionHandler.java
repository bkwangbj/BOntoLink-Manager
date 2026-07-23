package com.beiktech.bontolink.common;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常兜底：任何 Controller 未自行捕获的异常都会进到这里并写入日志，
 * 日志带上出错的「请求方法 + 路径」便于定位是哪个接口失败。
 *
 * <p>分级：400(可预期入参错误)记 warn 不带堆栈；500(未预期异常)记 error 带完整堆栈。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** "METHOD /uri" 字符串，用于在日志里标识是哪个接口出错 */
    private static String reqInfo(HttpServletRequest req) {
        return req == null ? "-" : req.getMethod() + " " + req.getRequestURI();
    }

    /** 参数非法(400)：业务可预期的入参错误，按 warn 记录(无需完整堆栈) */
    @ExceptionHandler(IllegalArgumentException.class)
    public R<?> badRequest(IllegalArgumentException e, HttpServletRequest req) {
        log.warn("bad request [{}]: {}", reqInfo(req), e.getMessage());
        return R.error(400, e.getMessage());
    }

    /** 兜底异常(500)：未被各 Controller 捕获的异常，按 error 记录完整堆栈便于排障 */
    @ExceptionHandler(Exception.class)
    public R<?> unknown(Exception e, HttpServletRequest req) {
        log.error("unhandled error [{}]", reqInfo(req), e);
        return R.error(500, e.getMessage() == null ? "server error" : e.getMessage());
    }
}
