package com.topos.admin.framework.security.handle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.topos.admin.common.constant.HttpStatus;
import com.topos.admin.common.core.domain.AdminResult;
import com.topos.admin.common.utils.ServletUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 授权失败：返回 AdminResult 403（配合 @PreAuthorize 等）。
 */
@Component
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public AccessDeniedHandlerImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        String msg = "没有权限，请联系管理员授权";
        ServletUtils.renderString(response, objectMapper.writeValueAsString(AdminResult.error(HttpStatus.FORBIDDEN, msg)));
    }
}
