package com.topos.strategy.wss.kafka;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * 仅在开启交易信号消费时启用 Kafka 基础设施。
 */
@Configuration
@EnableKafka
@ConditionalOnProperty(prefix = "topos.kafka.trade-signal", name = "enabled", havingValue = "true")
public class TradeSignalKafkaBootstrap {
}
