package com.shipyard.welding;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.shipyard.welding.mapper")
public class WeldingPermitApplication {

    public static void main(String[] args) {
        SpringApplication.run(WeldingPermitApplication.class, args);
    }
}
