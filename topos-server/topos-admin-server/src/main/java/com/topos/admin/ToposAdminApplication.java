package com.topos.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.topos")
@MapperScan({"com.topos.dal.mapper", "com.topos.admin.system.mapper"})
public class ToposAdminApplication {

	public static void main(String[] args) {
		SpringApplication.run(ToposAdminApplication.class, args);
	}
}
