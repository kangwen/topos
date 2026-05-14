package com.topos.web.support;

import jakarta.servlet.http.HttpServletRequest;

/** 解析真实客户端 IP（兼容反向代理常见头）。 */
public final class HttpRequestClientIp {

    private HttpRequestClientIp() {}

    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        String real = request.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) {
            return real.trim();
        }
        String addr = request.getRemoteAddr();
        return addr == null || addr.isBlank() ? "unknown" : addr.trim();
    }
}
