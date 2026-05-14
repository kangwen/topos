package com.topos.admin.framework.web.service;

import com.topos.admin.common.constant.Constants;
import com.topos.admin.common.core.domain.entity.SysRole;
import com.topos.admin.common.core.domain.entity.SysUser;
import com.topos.admin.common.utils.StringUtils;
import com.topos.admin.system.mapper.SysMenuMapper;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 角色与菜单权限：用户 ID 为 1 或角色 {@code admin} 视为全部权限 {@code *:*:*}，其余按菜单关联查询。
 */
@Service
public class AdminRbacService {

    private final SysMenuMapper sysMenuMapper;

    public AdminRbacService(SysMenuMapper sysMenuMapper) {
        this.sysMenuMapper = sysMenuMapper;
    }

    public Set<String> getRolePermission(SysUser user) {
        Set<String> roles = new HashSet<>();
        if (user == null) {
            return roles;
        }
        if (user.isAdmin()) {
            roles.add(Constants.SUPER_ADMIN);
            return roles;
        }
        if (user.getRoles() != null) {
            for (SysRole role : user.getRoles()) {
                if (StringUtils.isNotEmpty(role.getRoleKey())) {
                    roles.add(role.getRoleKey());
                }
                if (role.isAdmin()) {
                    roles.add(Constants.SUPER_ADMIN);
                }
            }
        }
        return roles;
    }

    public Set<String> getMenuPermission(SysUser user) {
        if (user == null) {
            return Set.of();
        }
        if (user.isAdmin()) {
            return Set.of(Constants.ALL_PERMISSION);
        }
        if (user.getRoles() != null) {
            for (SysRole role : user.getRoles()) {
                if (role.isAdmin()) {
                    return Set.of(Constants.ALL_PERMISSION);
                }
            }
        }
        List<String> perms = sysMenuMapper.selectMenuPermsByUserId(user.getUserId());
        if (perms == null || perms.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(perms);
    }
}
