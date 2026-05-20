package com.beiktech.bontolink.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 启动时执行 schema 与 data 脚本。
 * 由 application-{profile}.yml 通过 bontolink.db.* 注入实际脚本路径，
 * 保证 SQLite / PostgreSQL 切换无侵入。
 */
@Component
public class DatabaseInitializer {
    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Value("${bontolink.db.dialect}")
    private String dialect;

    @Value("${bontolink.db.schema-init}")
    private String schemaLocation;

    @Value("${bontolink.db.data-init}")
    private String dataLocation;

    @Autowired private ResourceLoader resourceLoader;
    @Autowired private JdbcTemplate jdbc;

    @PostConstruct
    public void init() {
        log.info("BOntoLink DB initializing — dialect={}", dialect);
        runScript(schemaLocation, "schema");
        runScript(dataLocation, "data");
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM ont_biz_category", Integer.class);
        log.info("BOntoLink DB initialized — ont_biz_category row count = {}", total);
    }

    private void runScript(String location, String tag) {
        try {
            Resource res = resourceLoader.getResource(location);
            if (!res.exists()) {
                log.warn("Init script not found: {}", location);
                return;
            }
            List<String> statements = readStatements(res);
            int ok = 0, failed = 0;
            for (String sql : statements) {
                String trimmed = sql.trim();
                if (trimmed.isEmpty()) continue;
                try {
                    jdbc.execute(trimmed);
                    ok++;
                } catch (Exception e) {
                    failed++;
                    log.error("[{}] STATEMENT FAILED: {} ... -> {}",
                            tag, abbreviate(trimmed), e.getMessage());
                }
            }
            log.info("[{}] {} statements executed, {} failed", tag, ok, failed);
        } catch (Exception e) {
            log.error("Failed to run script {} ({})", location, tag, e);
        }
    }

    private List<String> readStatements(Resource res) throws Exception {
        List<String> out = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                String stripped = stripInlineComment(line);
                String t = stripped.trim();
                if (t.isEmpty()) continue;
                buf.append(stripped).append('\n');
                if (t.endsWith(";")) {
                    String stmt = buf.toString();
                    int last = stmt.lastIndexOf(';');
                    out.add(last >= 0 ? stmt.substring(0, last) : stmt);
                    buf.setLength(0);
                }
            }
        }
        if (buf.toString().trim().length() > 0) {
            out.add(buf.toString());
        }
        return out;
    }

    /** 剥离行内 `--` 注释，但跳过单引号字符串内出现的 `--` */
    private String stripInlineComment(String line) {
        boolean inStr = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\'') {
                // 检查是否转义 ''
                if (inStr && i + 1 < line.length() && line.charAt(i + 1) == '\'') { i++; continue; }
                inStr = !inStr;
            } else if (!inStr && c == '-' && i + 1 < line.length() && line.charAt(i + 1) == '-') {
                return line.substring(0, i);
            }
        }
        return line;
    }

    private String abbreviate(String s) {
        return s.length() > 80 ? s.substring(0, 80).replace('\n', ' ') + "..." : s.replace('\n', ' ');
    }
}
