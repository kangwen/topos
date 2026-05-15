package com.topos.strategy.wss.heartbeat;

import com.topos.strategy.wss.props.WsProperties;
import com.topos.strategy.wss.session.WsSessionEntry;
import com.topos.strategy.wss.session.WsSessionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.WebSocketSession;

import java.nio.ByteBuffer;

/**
 * 周期性 WebSocket Ping，降低中间设备 idle 断开概率；与 {@link WsProperties#getMaxSessionIdleTimeoutMs()} 配合。
 */
@Component
public class WsHeartbeatScheduler {

	private static final Logger log = LoggerFactory.getLogger(WsHeartbeatScheduler.class);

	private final WsSessionRegistry registry;
	private final WsProperties wsProperties;

	public WsHeartbeatScheduler(WsSessionRegistry registry, WsProperties wsProperties) {
		this.registry = registry;
		this.wsProperties = wsProperties;
	}

	@Scheduled(fixedDelayString = "${topos.wss.ping-interval-ms:25000}")
	public void sendPingFrames() {
		long interval = wsProperties.getPingIntervalMs();
		if (interval <= 0) {
			return;
		}
		for (WsSessionEntry entry : registry.snapshotEntries()) {
			WebSocketSession session = entry.session();
			if (!session.isOpen()) {
				registry.unregisterSession(session);
				continue;
			}
			try {
				synchronized (session) {
					if (session.isOpen()) {
						session.sendMessage(new PingMessage(ByteBuffer.allocate(0)));
					}
				}
			} catch (Exception ex) {
				log.debug("ws ping failed sessionId={}: {}", session.getId(), ex.toString());
				registry.unregisterSession(session);
			}
		}
	}
}
