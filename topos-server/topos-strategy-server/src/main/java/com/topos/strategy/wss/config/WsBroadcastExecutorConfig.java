package com.topos.strategy.wss.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Kafka 监听线程只做入队；实际向 WS 会话发送在本线程池执行，避免阻塞 consumer。
 */
@Configuration
public class WsBroadcastExecutorConfig {

	@Bean(name = "wsBroadcastExecutor")
	public Executor wsBroadcastExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("ws-broadcast-");
		executor.setCorePoolSize(4);
		executor.setMaxPoolSize(32);
		executor.setQueueCapacity(10_000);
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.initialize();
		return executor;
	}
}
