package com.liuwentao.bililottery;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@MapperScan(basePackages = {"com.liuwentao.bililottery.mappers"})
@SpringBootApplication
public class BiliLotteryApplication {

    public static void main(String[] args) {
        SpringApplication.run(BiliLotteryApplication.class, args);
        System.out.println("开启");
    }

}
