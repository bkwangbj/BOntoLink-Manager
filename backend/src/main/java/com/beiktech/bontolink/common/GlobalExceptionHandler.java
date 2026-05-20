package com.beiktech.bontolink.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public R<?> badRequest(IllegalArgumentException e) {
        return R.error(400, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public R<?> unknown(Exception e) {
        log.error("unhandled error", e);
        return R.error(500, e.getMessage() == null ? "server error" : e.getMessage());
    }
}
