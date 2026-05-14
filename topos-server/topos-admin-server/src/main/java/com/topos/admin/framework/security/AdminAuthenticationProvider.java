package com.topos.admin.framework.security;

import cn.hutool.crypto.digest.MD5;
import com.topos.admin.common.core.domain.entity.SysUser;
import com.topos.admin.common.core.domain.model.LoginUser;
import com.topos.admin.framework.web.service.AdminUserDetailsService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 兼容 Flyway 种子数据中的 MD5（{@code MD5(loginName + password + salt)}）与 BCrypt 密码。
 */
@Component
public class AdminAuthenticationProvider extends DaoAuthenticationProvider {

    public AdminAuthenticationProvider(AdminUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        setUserDetailsService(userDetailsService);
        setPasswordEncoder(passwordEncoder);
    }

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails,
            org.springframework.security.authentication.UsernamePasswordAuthenticationToken authentication)
            throws AuthenticationException {
        if (authentication.getCredentials() == null) {
            throw new BadCredentialsException("Bad credentials");
        }
        String presentedPassword = authentication.getCredentials().toString();
        String storedPassword = userDetails.getPassword();
        if (storedPassword == null) {
            throw new BadCredentialsException("Bad credentials");
        }
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            if (!getPasswordEncoder().matches(presentedPassword, storedPassword)) {
                throw new BadCredentialsException("Bad credentials");
            }
            return;
        }
        if (!(userDetails instanceof LoginUser loginUser) || loginUser.getUser() == null) {
            throw new BadCredentialsException("Bad credentials");
        }
        SysUser u = loginUser.getUser();
        String salt = u.getSalt() == null ? "" : u.getSalt();
        String expected = MD5.create().digestHex(u.getLoginName() + presentedPassword + salt);
        if (!expected.equalsIgnoreCase(storedPassword)) {
            throw new BadCredentialsException("Bad credentials");
        }
    }
}
