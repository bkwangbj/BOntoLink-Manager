package com.beiktech.bontolink;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.beiktech.bontolink.mapper")
public class BontoLinkApplication {
    public static void main(String[] args) {
        SpringApplication.run(BontoLinkApplication.class, args);
    }
}
