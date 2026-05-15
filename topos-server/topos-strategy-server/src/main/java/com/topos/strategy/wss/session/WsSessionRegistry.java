package com.topos.strategy.wss.session;

import com.topos.strategy.wss.props.WsProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 进程内会话表：按客户端标识（字符串）索引；Kafka 广播在本 JVM 内遍历快照发送。
 */
@Component
public class WsSessionRegistry {

	private static final Logger log = LoggerFactory.getLogger(WsSessionRegistry.class);

	/** 握手后写入 {@link WebSocketSession#getAttributes()} 的 clientId（字符串）。 */
	public static final String ATTR_CLIENT_ID = "WS_CLIENT_ID";

	private final ConcurrentHashMap<String, WsSessionEntry> byClientId = new ConcurrentHashMap<>();
	private final WsProperties props;
	private final Executor broadcastExecutor;
	private final ObjectProvider<MeterRegistry> meterRegistry;

	public WsSessionRegistry(
			WsProperties props,
			@Qualifier("wsBroadcastExecutor") Executor broadcastExecutor,
			ObjectProvider<MeterRegistry> meterRegistry) {
		this.props = props;
		this.broadcastExecutor = broadcastExecutor;
		this.meterRegistry = meterRegistry;
	}

	/**
	 * 注册或替换会话；调用方应在握手已通过后执行。
	 *
	 * @return true 表示已登记；false 表示因容量或重复策略拒绝（新会话应已关闭）
	 */
	public boolean register(String clientId, WebSocketSession session) {
		if (clientId == null || clientId.isBlank()) {
			safeClose(session, CloseStatus.NOT_ACCEPTABLE);
			return false;
		}
		String key = clientId.trim();
		if (key.length() > props.getMaxClientIdLength()) {
			safeClose(session, CloseStatus.NOT_ACCEPTABLE);
			return false;
		}
		int max = props.getMaxSessions();
		if (max > 0 && byClientId.size() >= max && !byClientId.containsKey(key)) {
			safeClose(session, CloseStatus.POLICY_VIOLATION);
			return false;
		}
		WsSessionEntry entry = new WsSessionEntry(session);
		if (props.isClosePreviousSessionOnDuplicate()) {
			WsSessionEntry previous = byClientId.put(key, entry);
			if (previous != null) {
				safeClose(previous.session(), new CloseStatus(4401, "replaced by new connection"));
			}
			return true;
		}
		WsSessionEntry existing = byClientId.putIfAbsent(key, entry);
		if (existing != null) {
			safeClose(session, new CloseStatus(4400, "duplicate clientId"));
			return false;
		}
		return true;
	}

	public void removeByClientId(String clientId) {
		disconnectByClientId(clientId, CloseStatus.NORMAL);
	}

	/**
	 * 服务端主动断开指定客户端连接（从注册表移除并关闭 WebSocket）。
	 *
	 * @return 是否存在并已处理该 clientId 的会话
	 */
	public boolean disconnectByClientId(String clientId) {
		return disconnectByClientId(clientId, new CloseStatus(4402, "server disconnect"));
	}

	/**
	 * 服务端主动断开，可自定义 {@link CloseStatus}（code 建议使用 4xxx 应用自定义区间）。
	 */
	public boolean disconnectByClientId(String clientId, CloseStatus status) {
		if (clientId == null || clientId.isBlank()) {
			return false;
		}
		WsSessionEntry removed = byClientId.remove(clientId.trim());
		if (removed == null) {
			return false;
		}
		safeClose(removed.session(), status);
		return true;
	}

	/**
	 * 断开当前 JVM 内所有已登记连接（例如滚动发布前优雅踢线）。
	 *
	 * @return 实际关闭的会话数量
	 */
	public int disconnectAllClients(CloseStatus status) {
		List<String> ids = new ArrayList<>(byClientId.keySet());
		int n = 0;
		for (String id : ids) {
			if (disconnectByClientId(id, status)) {
				n++;
			}
		}
		return n;
	}

	/** 使用默认「服务端维护」原因批量断开。 */
	public int disconnectAllClients() {
		return disconnectAllClients(new CloseStatus(4403, "server maintenance"));
	}

	public void unregisterSession(WebSocketSession session) {
		String clientId = readClientId(session);
		if (clientId == null) {
			return;
		}
		byClientId.computeIfPresent(clientId, (k, entry) -> entry.session().getId().equals(session.getId()) ? null : entry);
	}

	public WsSessionEntry getEntry(String clientId) {
		return clientId == null ? null : byClientId.get(clientId.trim());
	}

	public int size() {
		return byClientId.size();
	}

	public void touchPong(WebSocketSession session) {
		String clientId = readClientId(session);
		if (clientId == null) {
			return;
		}
		WsSessionEntry e = byClientId.get(clientId);
		if (e != null && e.session().getId().equals(session.getId())) {
			e.touchPong();
		}
	}

	/**
	 * 异步广播：避免阻塞 Kafka 监听线程。
	 */
	public void broadcastTextAsync(String payload) {
		broadcastExecutor.execute(() -> broadcastTextSync(payload));
	}

	void broadcastTextSync(String payload) {
		WsSessionEntry[] snapshot = byClientId.values().toArray(WsSessionEntry[]::new);
		int batch = Math.max(1, props.getBroadcastBatchSize());
		AtomicInteger ok = new AtomicInteger();
		AtomicInteger fail = new AtomicInteger();
		for (int i = 0; i < snapshot.length; i += batch) {
			int end = Math.min(i + batch, snapshot.length);
			for (int j = i; j < end; j++) {
				sendOne(snapshot[j], payload, ok, fail);
			}
			Thread.yield();
		}
		meterRegistry.ifAvailable(reg -> {
			reg.counter("topos.wss.broadcast.deliveries", "result", "ok").increment(ok.get());
			reg.counter("topos.wss.broadcast.deliveries", "result", "fail").increment(fail.get());
		});
	}

	/**
	 * 供心跳调度器遍历当前快照。
	 */
	public List<WsSessionEntry> snapshotEntries() {
		return new ArrayList<>(byClientId.values());
	}

	private static String readClientId(WebSocketSession session) {
		Object attr = session.getAttributes().get(ATTR_CLIENT_ID);
		return attr instanceof String s && !s.isBlank() ? s.trim() : null;
	}

	private void sendOne(WsSessionEntry entry, String payload, AtomicInteger ok, AtomicInteger fail) {
		WebSocketSession session = entry.session();
		if (!session.isOpen()) {
			removeDead(entry);
			fail.incrementAndGet();
			return;
		}
		try {
			synchronized (session) {
				if (session.isOpen()) {
					session.sendMessage(new TextMessage(payload));
				}
			}
			ok.incrementAndGet();
		} catch (Exception ex) {
			log.debug("ws broadcast send failed sessionId={}: {}", session.getId(), ex.toString());
			removeDead(entry);
			fail.incrementAndGet();
		}
	}

	private void removeDead(WsSessionEntry entry) {
		WebSocketSession s = entry.session();
		String clientId = readClientId(s);
		if (clientId != null) {
			byClientId.computeIfPresent(clientId, (k, e) -> e.session().getId().equals(s.getId()) ? null : e);
		}
	}

	private static void safeClose(WebSocketSession session, CloseStatus status) {
		if (session == null || !session.isOpen()) {
			return;
		}
		try {
			session.close(status);
		} catch (IOException ignored) {
		}
	}
}
