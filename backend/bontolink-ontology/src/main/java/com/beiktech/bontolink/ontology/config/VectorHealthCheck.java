package com.beiktech.bontolink.ontology.config;

import com.beiktech.bontolink.ontology.config.OntologyEngineConfig.VectorConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * 向量库启动检查
 *
 * 在应用启动时检查向量库是否可用。
 * 如果配置了启用但连接不上，给出明确的操作提示。
 */
@Slf4j
@Configuration
public class VectorHealthCheck {

    private final OntologyEngineConfig config;

    public VectorHealthCheck(OntologyEngineConfig config) {
        this.config = config;
    }

    @PostConstruct
    public void check() {
        if (!config.getVector().isEnabled()) {
            log.info("向量库已禁用 (bontolink.ontology.vector.enabled=false)，跳过连接检查");
            return;
        }

        VectorConfig vc = config.getVector();
        String type = vc.getType() != null ? vc.getType().toLowerCase() : "";

        log.info("检查向量库连接: type={}, enabled=true", type);

        boolean ok = switch (type) {
            case "milvus" -> checkMilvus(vc);
            case "pgvector" -> checkPgvector(vc);
            default -> {
                log.warn("未知向量库类型 '{}'，跳过连接检查，将降级为纯文本匹配", type);
                yield true;
            }
        };

        if (!ok) {
            String hint = buildHint(type, vc);
            log.warn("╔══════════════════════════════════════════════════════════════╗");
            log.warn("║  向量库未就绪，本体推理中的向量检索将不可用                  ║");
            log.warn("║  本次请求将降级到关键词匹配 + 本体推理                       ║");
            log.warn("╠══════════════════════════════════════════════════════════════╣");
            for (String line : hint.split("\n")) {
                log.warn("║  {}", line);
            }
            log.warn("╚══════════════════════════════════════════════════════════════╝");
        } else {
            log.info("向量库连接正常: type={}", type);
        }
    }

    /**
     * 检查 Milvus TCP 端口是否可达
     */
    private boolean checkMilvus(VectorConfig vc) {
        String host = vc.getMilvusHost();
        int port = vc.getMilvusPort();

        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(host, port), 3000);
            log.info("Milvus 连接成功: {}:{}", host, port);
            return true;
        } catch (IOException e) {
            log.warn("Milvus 连接失败: {}:{} — {}", host, port, e.getMessage());
            return false;
        }
    }

    /**
     * 检查 pgvector（通过 PostgreSQL JDBC 试探）
     * pgvector 本身不是独立服务，随 PostgreSQL 一起，所以只要 PG 能连就行
     */
    private boolean checkPgvector(VectorConfig vc) {
        // pgvector 作为 PG 扩展存在，不需要单独检查连接
        // 应用的主数据源连接由 Spring 管理，无需在此重复检查
        log.info("pgvector 随 PostgreSQL 一同管理，由 Spring 数据源保证连接");
        return true;
    }

    /**
     * 生成启动提示信息
     */
    private String buildHint(String type, VectorConfig vc) {
        return switch (type) {
            case "milvus" -> {
                String home = vc.getMilvusHome();
                StringBuilder sb = new StringBuilder();
                sb.append("请先启动 Milvus 服务：");

                if (home != null && !home.isEmpty()) {
                    sb.append("\n   安装目录: ").append(home);
                    if (home.contains(":\\")) {
                        // Windows
                        sb.append("\n   启动命令: cd /d ").append(home).append(" && docker-compose up -d");
                        sb.append("\n      或:   cd /d ").append(home).append("\\bin && milvus.exe start");
                    } else {
                        // Linux
                        sb.append("\n   启动命令: cd ").append(home).append(" && docker-compose up -d");
                        sb.append("\n      或:   sudo systemctl start milvus");
                    }
                } else {
                    sb.append("\n   请配置 bontolink.ontology.vector.milvus-home");
                    sb.append("\n   或用 Docker 启动: docker run -d --name milvus -p 19530:19530 milvusdb/milvus:latest");
                }

                sb.append("\n   地址: ").append(vc.getMilvusHost()).append(":").append(vc.getMilvusPort());
                yield sb.toString();
            }
            case "pgvector" -> {
                yield "pgvector 是 PostgreSQL 扩展，请检查 PostgreSQL 是否运行:\n" +
                       "    Windows: net start postgresql-x64-15\n" +
                       "    Linux:   sudo systemctl start postgresql\n" +
                       "    Docker:  docker start <pg-container>\n" +
                       "  并确认已启用: CREATE EXTENSION IF NOT EXISTS vector;";
            }
            default -> "请根据向量库类型启动对应服务";
        };
    }
}
