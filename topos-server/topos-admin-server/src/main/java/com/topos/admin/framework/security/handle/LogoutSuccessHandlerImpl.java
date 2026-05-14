package com.topos.admin.framework.security.handle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.topos.admin.common.core.domain.AdminResult;
import com.topos.admin.common.core.domain.model.LoginUser;
import com.topos.admin.common.utils.ServletUtils;
import com.topos.admin.common.utils.StringUtils;
import com.topos.admin.framework.web.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 退出登录：清理 Redis 会话并返回 AdminResult
 */
@Component
public class LogoutSuccessHandlerImpl implements LogoutSuccessHandler {

    private final TokenService tokenService;
    private final ObjectMapper objectMapper;

    public LogoutSuccessHandlerImpl(TokenService tokenService, ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        LoginUser loginUser = tokenService.getLoginUser(request);
        if (StringUtils.isNotNull(loginUser)) {
            tokenService.delLoginUser(loginUser.getToken());
        }
        ServletUtils.renderString(response, objectMapper.writeValueAsString(AdminResult.success("退出成功")));
    }
}
