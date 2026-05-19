package com.topos.strategy.wss.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 本地 WebSocket 压测辅助接口开关（生产务必保持 {@code enabled=false}）。
 */
@ConfigurationProperties(prefix = "topos.wss.load-test")
public class WsLoadTestProperties {

	private boolean enabled = false;

	/** 签发压测 JWT 时的 subject 前缀。 */
	private String tokenSubjectPrefix = "ws-load-test";

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getTokenSubjectPrefix() {
		return tokenSubjectPrefix;
	}

	public void setTokenSubjectPrefix(String tokenSubjectPrefix) {
		this.tokenSubjectPrefix = tokenSubjectPrefix;
	}
}
