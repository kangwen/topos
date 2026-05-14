package com.topos.web.security;

/**
 * 解析 {@code Authorization} 头中的 Bearer token（scheme 大小写不敏感，可去掉重复的 {@code Bearer } 前缀）。
 */
public final class BearerTokens {

    private static final String PREFIX = "Bearer ";

    private BearerTokens() {
    }

    public static String fromAuthorizationHeader(String authorizationHeader) {
        if (authorizationHeader == null) {
            return null;
        }
        String h = authorizationHeader.trim();
        if (h.length() < PREFIX.length() || !h.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
            return null;
        }
        String token = h.substring(PREFIX.length()).trim();
        while (token.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
            token = token.substring(PREFIX.length()).trim();
        }
        return token.isEmpty() ? null : token;
    }
}
