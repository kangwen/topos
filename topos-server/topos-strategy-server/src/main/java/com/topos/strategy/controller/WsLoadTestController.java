package com.topos.strategy.controller;

import com.topos.common.api.Rsp;
import com.topos.strategy.controller.dto.WsLoadTestBroadcastRequest;
import com.topos.strategy.controller.dto.WsLoadTestTokenRequest;
import com.topos.strategy.wss.loadtest.WsLoadTestService;
import com.topos.strategy.wss.props.WsLoadTestProperties;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 本地压测专用：按 clientId 签发 JWT、触发进程内广播（Kafka 关闭时亦可测送达率）。
 */
@RestController
@RequestMapping("/api/v1/wss/load-test")
@ConditionalOnProperty(prefix = "topos.wss.load-test", name = "enabled", havingValue = "true")
public class WsLoadTestController {

	private final WsLoadTestService loadTestService;

	public WsLoadTestController(WsLoadTestService loadTestService) {
		this.loadTestService = loadTestService;
	}

	@PostMapping("/token")
	public Rsp<Map<String, Object>> mintToken(@Valid @RequestBody WsLoadTestTokenRequest request) {
		return Rsp.ok(loadTestService.mintToken(request.clientId()));
	}

	@PostMapping("/broadcast")
	public Rsp<Map<String, Object>> broadcast(@Valid @RequestBody(required = false) WsLoadTestBroadcastRequest request) {
		String traceId = request != null ? request.traceId() : null;
		Integer round = request != null ? request.round() : null;
		return Rsp.ok(loadTestService.broadcast(traceId, round));
	}

	@GetMapping("/sessions")
	public Rsp<Map<String, Object>> sessions() {
		return Rsp.ok(Map.of("activeSessions", loadTestService.activeSessions()));
	}
}
