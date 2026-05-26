package com.bjtu.canteen;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.bjtu.canteen.mapper")
public class CanteenSimApplication {
    public static void main(String[] args) {
        SpringApplication.run(CanteenSimApplication.class, args);
        System.out.println("==============================================");
        System.out.println("  北京交通大学就餐仿真系统 启动成功！");
        System.out.println("  前端访问: 打开 frontend/index.html");
        System.out.println("  后端接口: http://localhost:8888/api");
        System.out.println("==============================================");
    }
}
