package com.topos.strategy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * WebSocket 握手为 HTTP GET；浏览器无法自定义 Header，鉴权在 {@link com.topos.strategy.wss.handshake.WsJwtHandshakeInterceptor} 完成。
 */
@Configuration
public class WebSocketSecurityConfig {

	@Bean
	@Order(2)
	public SecurityFilterChain webSocketHandshakeChain(HttpSecurity http) throws Exception {
		http
				.securityMatcher("/ws/**")
				.csrf(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
		return http.build();
	}
}
