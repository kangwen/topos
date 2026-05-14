package com.topos.admin.system.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.topos.admin.common.core.domain.entity.SysUser;

/**
 * 用户表 {@code sys_user}。
 */
public interface SysUserMapper extends BaseMapper<SysUser> {

    default SysUser selectByLoginName(String loginName) {
        return selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getLoginName, loginName)
                .eq(SysUser::getDelFlag, "0"));
    }
}
