package com.topos.strategy.wss.session;

import com.topos.strategy.wss.props.WsProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WsSessionRegistryTest {

	private WsSessionRegistry registry;
	private WsProperties props;

	@BeforeEach
	void setUp() {
		props = new WsProperties();
		props.setMaxSessions(10_000);
		props.setClosePreviousSessionOnDuplicate(true);
		Executor direct = Executors.newSingleThreadExecutor();
		@SuppressWarnings("unchecked")
		ObjectProvider<MeterRegistry> meterRegistry = mock(ObjectProvider.class);
		registry = new WsSessionRegistry(props, direct, meterRegistry);
	}

	@Test
	void registerDuplicateClosesPrevious() throws Exception {
		String id = "client-a";
		WebSocketSession oldSession = mockSession(id, "old");
		WebSocketSession newSession = mockSession(id, "new");
		assertThat(registry.register(id, oldSession)).isTrue();
		assertThat(registry.register(id, newSession)).isTrue();
		verify(oldSession).close(any(CloseStatus.class));
		assertThat(registry.size()).isEqualTo(1);
	}

	@Test
	void disconnectByClientIdClosesAndRemoves() throws Exception {
		String id = "client-b";
		WebSocketSession session = mockSession(id, "s1");
		assertThat(registry.register(id, session)).isTrue();
		assertThat(registry.disconnectByClientId(id)).isTrue();
		verify(session).close(any(CloseStatus.class));
		assertThat(registry.size()).isEqualTo(0);
		assertThat(registry.disconnectByClientId(id)).isFalse();
	}

	@Test
	void registerDuplicateRejectKeepsOld() throws Exception {
		props.setClosePreviousSessionOnDuplicate(false);
		String id = "client-c";
		WebSocketSession oldSession = mockSession(id, "old");
		WebSocketSession newSession = mockSession(id, "new");
		assertThat(registry.register(id, oldSession)).isTrue();
		assertThat(registry.register(id, newSession)).isFalse();
		verify(oldSession, never()).close(any(CloseStatus.class));
		verify(newSession).close(any(CloseStatus.class));
		assertThat(registry.size()).isEqualTo(1);
	}

	@Test
	void disconnectAllClients() throws Exception {
		WebSocketSession s1 = mockSession("c1", "id1");
		WebSocketSession s2 = mockSession("c2", "id2");
		assertThat(registry.register("c1", s1)).isTrue();
		assertThat(registry.register("c2", s2)).isTrue();
		assertThat(registry.disconnectAllClients()).isEqualTo(2);
		verify(s1).close(any(CloseStatus.class));
		verify(s2).close(any(CloseStatus.class));
		assertThat(registry.size()).isEqualTo(0);
	}

	private static WebSocketSession mockSession(String clientId, String sessionId) {
		WebSocketSession s = mock(WebSocketSession.class);
		when(s.getId()).thenReturn(sessionId);
		when(s.isOpen()).thenReturn(true);
		HashMap<String, Object> attrs = new HashMap<>();
		attrs.put(WsSessionRegistry.ATTR_CLIENT_ID, clientId);
		when(s.getAttributes()).thenReturn(attrs);
		return s;
	}
}
