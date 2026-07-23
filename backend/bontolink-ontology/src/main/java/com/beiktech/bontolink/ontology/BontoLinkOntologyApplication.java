package com.beiktech.bontolink.ontology;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * BOntoLink 本体服务启动类
 * 提供：
 * 1. 本体信息查询（标准化 API）
 * 2. OWL 推理能力（Jena）
 * 3. 本体驱动的 SQL 生成
 * 4. 数据查询执行
 */
@SpringBootApplication
public class BontoLinkOntologyApplication {
    public static void main(String[] args) {
        SpringApplication.run(BontoLinkOntologyApplication.class, args);
    }
}
