package com.topos.strategy;

import com.topos.strategy.config.ApiSecurityProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = "com.topos")
@MapperScan("com.topos.dal.mapper")
@EnableConfigurationProperties({ApiSecurityProperties.class})
public class ToposStrategyApplication {

	public static void main(String[] args) {
		SpringApplication.run(ToposStrategyApplication.class, args);
	}
}
