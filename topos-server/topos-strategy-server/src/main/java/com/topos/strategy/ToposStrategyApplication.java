package com.topos.strategy;

import com.topos.strategy.config.ApiSecurityProperties;
import com.topos.strategy.wss.props.TradeSignalKafkaProperties;
import com.topos.strategy.wss.props.WsLoadTestProperties;
import com.topos.strategy.wss.props.WsProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.topos")
@MapperScan("com.topos.dal.mapper")
@EnableScheduling
@EnableConfigurationProperties({
		ApiSecurityProperties.class,
		WsProperties.class,
		WsLoadTestProperties.class,
		TradeSignalKafkaProperties.class})
public class ToposStrategyApplication {

	public static void main(String[] args) {
		SpringApplication.run(ToposStrategyApplication.class, args);
	}
}
