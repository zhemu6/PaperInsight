package com.zhemu.paperinsight;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.zhemu.paperinsight.mapper")
public class PaperInsightApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaperInsightApplication.class, args);
    }

}
