package com.topos.admin.framework.web.service;

import com.topos.admin.common.constant.Constants;
import com.topos.admin.common.core.domain.entity.SysRole;
import com.topos.admin.common.core.domain.model.LoginUser;
import com.topos.admin.common.utils.SecurityUtils;
import com.topos.admin.common.utils.StringUtils;
import com.topos.admin.framework.security.context.PermissionContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Set;

/**
 *  SpEL：@PreAuthorize("@ss.hasPermi('system:user:list')")。
 */
@Service("ss")
public class PermissionService {

    public boolean hasPermi(String permission) {
        if (StringUtils.isEmpty(permission)) {
            return false;
        }
        LoginUser loginUser = currentLoginUserOrNull();
        if (loginUser == null || CollectionUtils.isEmpty(loginUser.getPermissions())) {
            return false;
        }
        PermissionContextHolder.setContext(permission);
        return hasPermissions(loginUser.getPermissions(), permission);
    }

    public boolean lacksPermi(String permission) {
        return !hasPermi(permission);
    }

    public boolean hasAnyPermi(String permissions) {
        if (StringUtils.isEmpty(permissions)) {
            return false;
        }
        LoginUser loginUser = currentLoginUserOrNull();
        if (loginUser == null || CollectionUtils.isEmpty(loginUser.getPermissions())) {
            return false;
        }
        PermissionContextHolder.setContext(permissions);
        Set<String> authorities = loginUser.getPermissions();
        for (String permission : permissions.split(Constants.PERMISSION_DELIMITER)) {
            if (permission != null && hasPermissions(authorities, permission.trim())) {
                return true;
            }
        }
        return false;
    }

    public boolean hasRole(String role) {
        if (StringUtils.isEmpty(role)) {
            return false;
        }
        LoginUser loginUser = currentLoginUserOrNull();
        if (loginUser == null || loginUser.getUser() == null || loginUser.getUser().getRoles() == null) {
            return false;
        }
        for (SysRole sysRole : loginUser.getUser().getRoles()) {
            String roleKey = sysRole.getRoleKey();
            if (Constants.SUPER_ADMIN.equals(roleKey) || roleKey.equals(StringUtils.trim(role))) {
                return true;
            }
        }
        return false;
    }

    public boolean lacksRole(String role) {
        return !hasRole(role);
    }

    public boolean hasAnyRoles(String roles) {
        if (StringUtils.isEmpty(roles)) {
            return false;
        }
        for (String role : roles.split(Constants.ROLE_DELIMITER)) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasPermissions(Set<String> permissions, String permission) {
        return permissions.contains(Constants.ALL_PERMISSION) || permissions.contains(StringUtils.trim(permission));
    }

    private LoginUser currentLoginUserOrNull() {
        try {
            return SecurityUtils.getLoginUser();
        } catch (Exception e) {
            return null;
        }
    }
}
