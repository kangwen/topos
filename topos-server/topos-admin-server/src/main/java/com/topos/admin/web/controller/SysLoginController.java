package com.topos.admin.web.controller;

import com.topos.admin.common.constant.Constants;
import com.topos.admin.common.core.domain.AdminResult;
import com.topos.admin.common.core.domain.entity.SysUser;
import com.topos.admin.common.core.domain.model.LoginBody;
import com.topos.admin.common.core.domain.model.LoginUser;
import com.topos.admin.common.utils.SecurityUtils;
import com.topos.admin.framework.web.service.AdminLoginService;
import com.topos.admin.framework.web.service.AdminMenuService;
import com.topos.admin.framework.web.service.AdminRbacService;
import com.topos.admin.framework.web.service.TokenService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 登录与用户信息接口
 */
@RestController
public class SysLoginController {

    private final AdminLoginService loginService;
    private final AdminRbacService adminRbacService;
    private final AdminMenuService adminMenuService;
    private final TokenService tokenService;

    public SysLoginController(
            AdminLoginService loginService,
            AdminRbacService adminRbacService,
            AdminMenuService adminMenuService,
            TokenService tokenService) {
        this.loginService = loginService;
        this.adminRbacService = adminRbacService;
        this.adminMenuService = adminMenuService;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public AdminResult login(@RequestBody LoginBody loginBody) {
        AdminResult result = AdminResult.success();
        String token = loginService.login(
                loginBody.getUsername(),
                loginBody.getPassword(),
                loginBody.getCode(),
                loginBody.getUuid());
        result.put(Constants.TOKEN, token);
        return result;
    }

    @GetMapping("/getInfo")
    public AdminResult getInfo() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        SysUser user = loginUser.getUser();
        Set<String> roles = adminRbacService.getRolePermission(user);
        Set<String> permissions = adminRbacService.getMenuPermission(user);
        if (!Objects.equals(new HashSet<>(nullSafe(loginUser.getPermissions())), new HashSet<>(nullSafe(permissions)))) {
            loginUser.setPermissions(permissions);
            tokenService.refreshToken(loginUser);
        }
        AdminResult result = AdminResult.success();
        result.put("user", safeUser(user));
        result.put("roles", roles);
        result.put("permissions", permissions);
        result.put("isDefaultModifyPwd", false);
        result.put("isPasswordExpired", false);
        return result;
    }

    @GetMapping("/getRouters")
    public AdminResult getRouters() {
        return AdminResult.success(adminMenuService.buildRouters(SecurityUtils.getLoginUser()));
    }

    private static Set<String> nullSafe(Set<String> s) {
        return s == null ? Set.of() : s;
    }

    private static SysUser safeUser(SysUser source) {
        if (source == null) {
            return null;
        }
        SysUser target = new SysUser();
        BeanUtils.copyProperties(source, target);
        target.setPassword(null);
        return target;
    }
}
