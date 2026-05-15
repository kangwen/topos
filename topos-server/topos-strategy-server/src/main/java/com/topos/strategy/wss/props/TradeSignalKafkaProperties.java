package com.topos.strategy.wss.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 交易信号 Kafka 消费（每实例独立 group 才能全量广播到各 strategy-server）。
 */
@ConfigurationProperties(prefix = "topos.kafka.trade-signal")
public class TradeSignalKafkaProperties {

	private boolean enabled = false;

	private int listenerConcurrency = 1;

	/**
	 * 消费 topic，与策略引擎生产端一致。
	 */
	private String topic = "topos.trade-signals";

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public int getListenerConcurrency() {
		return listenerConcurrency;
	}

	public void setListenerConcurrency(int listenerConcurrency) {
		this.listenerConcurrency = listenerConcurrency;
	}
}
