package com.beiktech.bontolink.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        ContentCachingRequestWrapper wrapped = new ContentCachingRequestWrapper(request);
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(wrapped, response);
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            String uri = wrapped.getRequestURI();
            String query = wrapped.getQueryString();
            String url = query != null ? uri + "?" + query : uri;

            byte[] bodyBytes = wrapped.getContentAsByteArray();
            if (bodyBytes.length > 0) {
                String body = new String(bodyBytes, StandardCharsets.UTF_8);
                if (body.length() > 1000) {
                    body = body.substring(0, 1000) + "...(truncated)";
                }
                log.info("[{}] {} | body={} | {}ms | {}", wrapped.getMethod(), url, body, elapsed, response.getStatus());
            } else {
                log.info("[{}] {} | {}ms | {}", wrapped.getMethod(), url, elapsed, response.getStatus());
            }
        }
    }
}
