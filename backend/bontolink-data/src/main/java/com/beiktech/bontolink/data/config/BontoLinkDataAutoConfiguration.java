package com.beiktech.bontolink.data.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * BOntoLink 数据层自动配置
 * 供 bontolink-admin 和 bontolink-ontology-service 模块引用
 */
@Configuration
@MapperScan("com.beiktech.bontolink.data.mapper")
public class BontoLinkDataAutoConfiguration {
    // MyBatis Mapper 扫描配置
    // DataSource 由应用模块自己配置（application.yml）
}
