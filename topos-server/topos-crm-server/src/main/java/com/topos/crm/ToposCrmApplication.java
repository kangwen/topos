package com.topos.crm;

import com.topos.crm.config.ApiSecurityProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = "com.topos")
@MapperScan("com.topos.dal.mapper")
@EnableConfigurationProperties({ApiSecurityProperties.class})
public class ToposCrmApplication {

	public static void main(String[] args) {
		SpringApplication.run(ToposCrmApplication.class, args);
	}
}
