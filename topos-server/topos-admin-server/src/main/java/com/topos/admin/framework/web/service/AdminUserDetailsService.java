package com.topos.admin.framework.web.service;

import com.topos.admin.common.core.domain.entity.SysRole;
import com.topos.admin.common.core.domain.entity.SysUser;
import com.topos.admin.common.core.domain.model.LoginUser;
import com.topos.admin.system.mapper.SysRoleMapper;
import com.topos.admin.system.mapper.SysUserMapper;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 从 {@code sys_user}、{@code sys_user_role}、{@code sys_role} 加载管理员。
 */
@Service
public class AdminUserDetailsService implements UserDetailsService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;

    public AdminUserDetailsService(SysUserMapper sysUserMapper, SysRoleMapper sysRoleMapper) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserMapper.selectByLoginName(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        if (!"0".equals(user.getStatus())) {
            throw new DisabledException("用户已停用");
        }
        List<SysRole> roles = sysRoleMapper.selectRolesByUserId(user.getUserId());
        user.setRoles(roles);
        return new LoginUser(user.getUserId(), user.getDeptId(), user, Set.of());
    }
}
