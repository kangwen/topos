package com.topos.admin.common.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户表 {@code sys_user}；JSON 与 ruoyi-vue 对齐：{@code userName} 为登录名，{@code nickName} 为昵称。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends SysBaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;

    private Long deptId;

    @TableField("login_name")
    @JsonProperty("userName")
    private String loginName;

    @TableField("user_name")
    @JsonProperty("nickName")
    private String nickName;
    private String userType;
    private String email;
    private String phonenumber;
    private String sex;
    private String avatar;

    /** 仅请求体写入，响应中不返回 */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String salt;
    private String status;
    private String delFlag;
    private String loginIp;
    private LocalDateTime loginDate;
    private LocalDateTime pwdUpdateDate;

    @TableField(exist = false)
    private List<SysRole> roles;

    @TableField(exist = false)
    private SysDept dept;

    @TableField(exist = false)
    private Long[] roleIds;

    @TableField(exist = false)
    private Long[] postIds;

    public boolean isAdmin() {
        return userId != null && userId == 1L;
    }
}
