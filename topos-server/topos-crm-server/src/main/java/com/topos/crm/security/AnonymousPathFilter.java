package com.topos.crm.security;

import com.topos.crm.config.ApiSecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 根据 {@link ApiSecurityProperties#getAnonymousPaths()} 判定当前请求是否允许免登录访问：
 * 命中时在请求域写入标记，供 {@link JwtAuthenticationFilter} 跳过 JWT 解析（避免匿名接口带无效 Token 被清上下文）。
 */
public class AnonymousPathFilter extends OncePerRequestFilter {

	public static final String REQUEST_ATTR_USER_ANONYMOUS_ALLOWED =
			AnonymousPathFilter.class.getName() + ".anonymousAllowed";

	private final ApiSecurityProperties properties;
	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	public AnonymousPathFilter(ApiSecurityProperties properties) {
		this.properties = properties;
	}

	public static boolean isAnonymousAllowed(HttpServletRequest request) {
		return Boolean.TRUE.equals(request.getAttribute(REQUEST_ATTR_USER_ANONYMOUS_ALLOWED));
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String path = ApiRequestPaths.pathWithinApplication(request);
		if (matchesAnonymous(path)) {
			request.setAttribute(REQUEST_ATTR_USER_ANONYMOUS_ALLOWED, Boolean.TRUE);
		}
		filterChain.doFilter(request, response);
	}

	private boolean matchesAnonymous(String path) {
		for (String pattern : properties.getAnonymousPaths()) {
			if (pattern == null || pattern.isBlank()) {
				continue;
			}
			if (pathMatcher.match(pattern.trim(), path)) {
				return true;
			}
		}
		return false;
	}
}
