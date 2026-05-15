package com.topos.strategy.wss.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 与 Kafka / 下行 WS 文本 JSON 对齐的载荷示例（策略引擎可按需扩展字段）。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TradeSignalPayload(
		String traceId,
		String symbol,
		String side,
		String price,
		Long eventTimeMs
) {
}
