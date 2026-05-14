package com.topos.admin.framework.security.context;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * 在请求范围内记录当前权限表达式上下文（数据权限等可扩展）。
 */
public final class PermissionContextHolder {

    private static final String PERMISSION_CONTEXT_ATTRIBUTES = "PERMISSION_CONTEXT";

    private PermissionContextHolder() {
    }

    public static void setContext(String permission) {
        RequestContextHolder.currentRequestAttributes().setAttribute(
                PERMISSION_CONTEXT_ATTRIBUTES,
                permission,
                RequestAttributes.SCOPE_REQUEST);
    }

    public static String getContext() {
        Object v = RequestContextHolder.currentRequestAttributes().getAttribute(
                PERMISSION_CONTEXT_ATTRIBUTES,
                RequestAttributes.SCOPE_REQUEST);
        return v != null ? v.toString() : null;
    }
}
