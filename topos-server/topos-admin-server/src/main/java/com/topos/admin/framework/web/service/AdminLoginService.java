package com.topos.admin.framework.web.service;

import com.topos.admin.common.core.domain.model.LoginUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * 登录编排
 */
@Service
public class AdminLoginService {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final AdminCaptchaService adminCaptchaService;

    @Value("${topos.captcha.enabled:false}")
    private boolean captchaEnabled;

    public AdminLoginService(
            AuthenticationManager authenticationManager,
            TokenService tokenService,
            AdminCaptchaService adminCaptchaService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.adminCaptchaService = adminCaptchaService;
    }

    public String login(String username, String password, String code, String uuid) {
        if (captchaEnabled) {
            adminCaptchaService.validateAndConsume(uuid, code);
        }
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        if (loginUser.getUser() != null) {
            loginUser.getUser().setPassword(null);
            loginUser.getUser().setSalt(null);
        }
        return tokenService.createToken(loginUser);
    }
}
