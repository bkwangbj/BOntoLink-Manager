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
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

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

            // 收集所有参数（query + form）
            Map<String, String[]> params = wrapped.getParameterMap();
            String paramsStr = params.isEmpty() ? null : params.entrySet().stream()
                    .map(e -> e.getKey() + "=" + Arrays.toString(e.getValue()))
                    .collect(Collectors.joining(", "));

            byte[] bodyBytes = wrapped.getContentAsByteArray();
            String body = null;
            if (bodyBytes.length > 0) {
                body = new String(bodyBytes, StandardCharsets.UTF_8);
                if (body.length() > 1000) {
                    body = body.substring(0, 1000) + "...(truncated)";
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append("[").append(wrapped.getMethod()).append("] ").append(url);
            if (paramsStr != null) sb.append(" | params={").append(paramsStr).append("}");
            if (body != null)     sb.append(" | body=").append(body);
            sb.append(" | ").append(elapsed).append("ms | ").append(response.getStatus());
            log.info("{}", sb);
        }
    }
}
