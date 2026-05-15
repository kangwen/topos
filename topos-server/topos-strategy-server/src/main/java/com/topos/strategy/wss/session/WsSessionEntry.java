package com.topos.strategy.wss.session;

import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 单连接元数据：便于心跳与应用层 pong 观测。
 */
public final class WsSessionEntry {

	private final WebSocketSession session;
	private final AtomicLong lastPongAtEpochMs = new AtomicLong(System.currentTimeMillis());

	public WsSessionEntry(WebSocketSession session) {
		this.session = session;
	}

	public WebSocketSession session() {
		return session;
	}

	public void touchPong() {
		lastPongAtEpochMs.set(System.currentTimeMillis());
	}

	public long lastPongAtEpochMs() {
		return lastPongAtEpochMs.get();
	}
}
