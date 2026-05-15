package com.topos.strategy.wss.handshake;

import com.topos.common.security.jwt.JwtTokenProvider;
import com.topos.strategy.wss.props.WsProperties;
import com.topos.strategy.wss.session.WsSessionRegistry;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * 握手阶段完成 JWT 校验并解析客户端标识（浏览器 WS 无法自定义 Header，使用 query {@code access_token}）。
 */
@Component
public class WsJwtHandshakeInterceptor implements HandshakeInterceptor {

	private static final Logger log = LoggerFactory.getLogger(WsJwtHandshakeInterceptor.class);

	public static final String ATTR_ACCESS_TOKEN = "WS_ACCESS_TOKEN";

	private final JwtTokenProvider jwtTokenProvider;
	private final WsProperties wsProperties;

	public WsJwtHandshakeInterceptor(JwtTokenProvider jwtTokenProvider, WsProperties wsProperties) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.wsProperties = wsProperties;
	}

	@Override
	public boolean beforeHandshake(
			ServerHttpRequest request,
			ServerHttpResponse response,
			WebSocketHandler wsHandler,
			java.util.Map<String, Object> attributes) {
		if (!(request instanceof ServletServerHttpRequest servletRequest)) {
			return false;
		}
		String token = servletRequest.getServletRequest().getParameter("access_token");
		if (token == null || token.isBlank()) {
			log.warn("ws handshake rejected: missing access_token");
			return false;
		}
		try {
			Claims claims = jwtTokenProvider.parse(token).getPayload();
			String clientId = resolveClientId(claims);
			if (clientId == null) {
				log.warn("ws handshake rejected: cannot resolve client id from token");
				return false;
			}
			attributes.put(WsSessionRegistry.ATTR_CLIENT_ID, clientId);
			attributes.put(ATTR_ACCESS_TOKEN, token);
			return true;
		} catch (JwtException ex) {
			log.warn("ws handshake rejected: invalid token: {}", ex.getMessage());
			return false;
		}
	}

	private String resolveClientId(Claims claims) {
		String claimName = wsProperties.getClientIdClaim();
		if (claimName != null && !claimName.isBlank()) {
			Object raw = claims.get(claimName);
			if (raw != null) {
				String s = raw.toString().trim();
				if (isValidClientId(s)) {
					return s;
				}
			}
		}
		String sub = claims.getSubject();
		if (sub != null) {
			String s = sub.trim();
			if (isValidClientId(s)) {
				return s;
			}
		}
		return null;
	}

	private boolean isValidClientId(String s) {
		return !s.isEmpty() && s.length() <= wsProperties.getMaxClientIdLength();
	}

	@Override
	public void afterHandshake(
			ServerHttpRequest request,
			ServerHttpResponse response,
			WebSocketHandler wsHandler,
			Exception exception) {
	}
}
