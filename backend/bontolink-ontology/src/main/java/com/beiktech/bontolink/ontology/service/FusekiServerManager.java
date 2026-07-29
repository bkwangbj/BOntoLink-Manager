package com.beiktech.bontolink.ontology.service;

import com.beiktech.bontolink.ontology.config.OntologyEngineConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.springframework.stereotype.Service;

/**
 * 嵌入式 Fuseki 服务器管理器
 *
 * 在 Spring Boot 进程内启动 Fuseki，将当前 OntModel 暴露为标准 SPARQL 1.1 端点。
 * OntologyModelManager 每次重建完成后调用 syncModel() 刷新 dataset。
 */
@Slf4j
@Service
public class FusekiServerManager {

    private final OntologyEngineConfig config;

    private volatile FusekiServer fusekiServer;
    private final Dataset dataset = DatasetFactory.createGeneral();

    public FusekiServerManager(OntologyEngineConfig config) {
        this.config = config;
    }

    @PostConstruct
    public void start() {
        OntologyEngineConfig.FusekiConfig fusekiConfig = config.getFuseki();
        if (!fusekiConfig.isEnabled()) {
            log.info("嵌入式 Fuseki 已禁用 (bontolink.ontology.fuseki.enabled=false)");
            return;
        }

        try {
            String datasetPath = "/" + fusekiConfig.getDatasetName();
            fusekiServer = FusekiServer.create()
                    .port(fusekiConfig.getPort())
                    .add(datasetPath, dataset)
                    .enablePing(true)
                    .enableStats(true)
                    .enableMetrics(true)
                    .enableCors(true)  // 启用内置 CORS 支持
                    .build();
            fusekiServer.start();
            log.info("嵌入式 Fuseki 已启动: http://localhost:{}{}/sparql  (管理端点: http://localhost:{}/$/)",
                    fusekiConfig.getPort(), datasetPath, fusekiConfig.getPort());
        } catch (Exception e) {
            log.error("嵌入式 Fuseki 启动失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 将新 OntModel 同步到 Fuseki dataset（OntologyModelManager 重建后调用）
     */
    public void syncModel(Model model) {
        if (fusekiServer == null || model == null) return;
        try {
            dataset.executeWrite(() -> {
                Model defaultModel = dataset.getDefaultModel();
                defaultModel.removeAll();
                defaultModel.add(model);
            });
            log.info("Fuseki dataset 已同步，语句数: {}", model.size());
        } catch (Exception e) {
            log.warn("Fuseki dataset 同步失败: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void stop() {
        if (fusekiServer != null) {
            fusekiServer.stop();
            log.info("嵌入式 Fuseki 已停止");
        }
    }

    public boolean isRunning() {
        return fusekiServer != null;
    }

    public int getPort() {
        return config.getFuseki().getPort();
    }

    public String getDatasetName() {
        return config.getFuseki().getDatasetName();
    }
}
