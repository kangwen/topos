package com.topos.web.config;

import com.topos.web.security.AnonymousPathFilter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 安全配置：免登录路径等（与 application.yml 中 topos.security.strategy 对齐）。
 */
@ConfigurationProperties(prefix = "topos.security.strategy")
public class ApiSecurityProperties {

	/**
	 * Ant 风格路径模式；命中则视为免登录（由 {@link AnonymousPathFilter} 标记，
	 * {@link org.springframework.security.web.SecurityFilterChain} 中需同时 permitAll）。
	 */
	private List<String> anonymousPaths = new ArrayList<>(List.of(
			"/strategy/**"
	));

	public List<String> getAnonymousPaths() {
		return anonymousPaths;
	}

	public void setAnonymousPaths(List<String> anonymousPaths) {
		this.anonymousPaths = anonymousPaths != null ? anonymousPaths : new ArrayList<>();
	}
}
