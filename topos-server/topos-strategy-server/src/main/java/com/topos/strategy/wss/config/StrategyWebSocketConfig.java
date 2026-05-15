package com.topos.strategy.wss.config;

import com.topos.strategy.wss.handshake.WsJwtHandshakeInterceptor;
import com.topos.strategy.wss.handler.StrategyWebSocketHandler;
import com.topos.strategy.wss.props.WsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * WebSocket 端点注册与 Servlet 容器参数（空闲超时、缓冲）。
 */
@Configuration
@EnableWebSocket
public class StrategyWebSocketConfig implements WebSocketConfigurer {

	private final StrategyWebSocketHandler webSocketHandler;
	private final WsJwtHandshakeInterceptor handshakeInterceptor;
	private final WsProperties wsProperties;

	public StrategyWebSocketConfig(
			StrategyWebSocketHandler webSocketHandler,
			WsJwtHandshakeInterceptor handshakeInterceptor,
			WsProperties wsProperties) {
		this.webSocketHandler = webSocketHandler;
		this.handshakeInterceptor = handshakeInterceptor;
		this.wsProperties = wsProperties;
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		String[] origins = wsProperties.getAllowedOriginPatterns().toArray(String[]::new);
		registry.addHandler(webSocketHandler, wsProperties.getEndpointPath())
				.addInterceptors(handshakeInterceptor)
				.setAllowedOriginPatterns(origins);
	}

	@Bean
	public ServletServerContainerFactoryBean createWebSocketContainer(WsProperties props) {
		ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
		container.setMaxTextMessageBufferSize(props.getMaxTextMessageBufferSize());
		if (props.getMaxSessionIdleTimeoutMs() > 0) {
			container.setMaxSessionIdleTimeout(props.getMaxSessionIdleTimeoutMs());
		}
		return container;
	}
}
