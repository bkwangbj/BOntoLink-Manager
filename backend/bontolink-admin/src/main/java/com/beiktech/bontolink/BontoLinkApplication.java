package com.beiktech.bontolink;

import com.alicp.jetcache.anno.config.EnableCreateCacheAnnotation;
import com.alicp.jetcache.anno.config.EnableMethodCache;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableMethodCache(basePackages = "com.beiktech.bontolink.service")
@EnableCreateCacheAnnotation
public class BontoLinkApplication {
    public static void main(String[] args) {
        SpringApplication.run(BontoLinkApplication.class, args);
    }
}
