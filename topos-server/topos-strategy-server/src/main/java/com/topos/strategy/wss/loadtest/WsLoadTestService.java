package com.topos.strategy.wss.loadtest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.topos.common.exception.BizException;
import com.topos.common.security.jwt.JwtTokenProvider;
import com.topos.strategy.wss.props.WsLoadTestProperties;
import com.topos.strategy.wss.props.WsProperties;
import com.topos.strategy.wss.session.WsSessionRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "topos.wss.load-test", name = "enabled", havingValue = "true")
public class WsLoadTestService {

	private final WsLoadTestProperties loadTestProperties;
	private final WsProperties wsProperties;
	private final WsSessionRegistry registry;
	private final JwtTokenProvider jwtTokenProvider;
	private final ObjectMapper objectMapper;

	public WsLoadTestService(
			WsLoadTestProperties loadTestProperties,
			WsProperties wsProperties,
			WsSessionRegistry registry,
			JwtTokenProvider jwtTokenProvider,
			ObjectMapper objectMapper) {
		this.loadTestProperties = loadTestProperties;
		this.wsProperties = wsProperties;
		this.registry = registry;
		this.jwtTokenProvider = jwtTokenProvider;
		this.objectMapper = objectMapper;
	}

	public Map<String, Object> mintToken(String clientId) {
		requireEnabled();
		String normalized = normalizeClientId(clientId);
		Map<String, Object> claims = new LinkedHashMap<>();
		claims.put("aud", "CLIENT");
		claims.put(wsProperties.getClientIdClaim(), normalized);
		String subject = loadTestProperties.getTokenSubjectPrefix() + ":" + normalized;
		String token = jwtTokenProvider.createToken(subject, claims);
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("tokenType", "Bearer");
		body.put("accessToken", token);
		body.put("clientId", normalized);
		return body;
	}

	public Map<String, Object> broadcast(String traceId, Integer round) {
		requireEnabled();
		String id = traceId == null || traceId.isBlank() ? UUID.randomUUID().toString() : traceId.trim();
		long sentAtMs = System.currentTimeMillis();
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("type", "load_test_signal");
		payload.put("traceId", id);
		if (round != null) {
			payload.put("round", round);
		}
		payload.put("sentAtMs", sentAtMs);
		String json = toJson(payload);
		registry.broadcastTextAsync(json);
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("traceId", id);
		body.put("sentAtMs", sentAtMs);
		body.put("activeSessions", registry.size());
		body.put("payload", payload);
		return body;
	}

	public int activeSessions() {
		requireEnabled();
		return registry.size();
	}

	private void requireEnabled() {
		if (!loadTestProperties.isEnabled()) {
			throw new BizException(403, "ws load-test API is disabled");
		}
	}

	private String normalizeClientId(String clientId) {
		if (clientId == null || clientId.isBlank()) {
			throw new BizException(400, "clientId is required");
		}
		String normalized = clientId.trim();
		if (normalized.length() > wsProperties.getMaxClientIdLength()) {
			throw new BizException(400, "clientId too long");
		}
		return normalized;
	}

	private String toJson(Map<String, Object> payload) {
		try {
			return objectMapper.writeValueAsString(payload);
		} catch (JsonProcessingException ex) {
			throw new BizException(500, "failed to serialize broadcast payload");
		}
	}
}
