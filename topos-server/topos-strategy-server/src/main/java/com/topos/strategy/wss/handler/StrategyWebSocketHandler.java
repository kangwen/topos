package com.topos.strategy.wss.handler;

import com.topos.strategy.wss.session.WsSessionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 建连后写入 {@link WsSessionRegistry}；处理应用层 {@code {"type":"pong"}}。
 */
@Component
public class StrategyWebSocketHandler extends TextWebSocketHandler {

	private static final Logger log = LoggerFactory.getLogger(StrategyWebSocketHandler.class);

	private static final String JSON_PONG = "{\"type\":\"pong\"}";

	private final WsSessionRegistry registry;

	public StrategyWebSocketHandler(WsSessionRegistry registry) {
		this.registry = registry;
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		Object idObj = session.getAttributes().get(WsSessionRegistry.ATTR_CLIENT_ID);
		if (!(idObj instanceof String clientId) || clientId.isBlank()) {
			log.warn("ws connection closed: missing client id in attributes sessionId={}", session.getId());
			closeQuietly(session, CloseStatus.NOT_ACCEPTABLE);
			return;
		}
		boolean ok = registry.register(clientId, session);
		if (!ok) {
			log.warn("ws connection rejected: register failed clientId={} sessionId={}", clientId, session.getId());
			closeQuietly(session, CloseStatus.POLICY_VIOLATION);
		}
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) {
		String p = message.getPayload();
		if (p == null) {
			return;
		}
		String trimmed = p.trim();
		if (trimmed.equalsIgnoreCase("PONG") || trimmed.equals(JSON_PONG)) {
			registry.touchPong(session);
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		registry.unregisterSession(session);
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) {
		log.debug("ws transport error sessionId={}: {}", session.getId(), exception.toString());
		registry.unregisterSession(session);
	}

	private void closeQuietly(WebSocketSession session, CloseStatus status) {
		try {
			session.close(status);
		} catch (Exception ignored) {
		}
	}
}
