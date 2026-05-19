package com.topos.strategy.controller.dto;

import jakarta.validation.constraints.Size;

public record WsLoadTestBroadcastRequest(
		@Size(max = 128) String traceId,
		Integer round) {
}
