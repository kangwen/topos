package com.topos.strategy.wss.kafka;

import com.topos.strategy.wss.session.WsSessionRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * 各 strategy-server 实例独立 consumer group，保证每条交易信号到达每个 JVM，再本地广播到所有 WS。
 */
@Component
@ConditionalOnProperty(prefix = "topos.kafka.trade-signal", name = "enabled", havingValue = "true")
public class TradeSignalKafkaListener {

	private final WsSessionRegistry registry;

	public TradeSignalKafkaListener(WsSessionRegistry registry) {
		this.registry = registry;
	}

	@KafkaListener(
			topics = "${topos.kafka.trade-signal.topic}",
			groupId = "${spring.kafka.consumer.group-id}",
			concurrency = "${topos.kafka.trade-signal.listener-concurrency:1}")
	public void onTradeSignal(@Payload String payload) {
		if (payload == null || payload.isEmpty()) {
			return;
		}
		registry.broadcastTextAsync(payload);
	}
}
