package com.zhiyuan.college;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.zhiyuan.college.mapper")
public class CollegeRecommendationApplication {

    public static void main(String[] args) {
        SpringApplication.run(CollegeRecommendationApplication.class, args);
    }
}
