package com.topos.strategy.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 与 Spring Security {@code requestMatchers} 尽量一致的应用内路径（servletPath + pathInfo，否则回退 URI - contextPath）。
 */
public final class ApiRequestPaths {

    private ApiRequestPaths() {
    }

    public static String pathWithinApplication(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        String pathInfo = request.getPathInfo();
        StringBuilder sb = new StringBuilder();
        if (servletPath != null) {
            sb.append(servletPath);
        }
        if (pathInfo != null) {
            sb.append(pathInfo);
        }
        String combined = sb.toString();
        if (!combined.isEmpty()) {
            return combined.startsWith("/") ? combined : "/" + combined;
        }
        String uri = request.getRequestURI();
        if (uri == null) {
            return "/";
        }
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        return uri.isEmpty() ? "/" : uri;
    }
}
