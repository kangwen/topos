package com.topos.admin.common.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.topos.admin.common.constant.Constants;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色表 {@code sys_role}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends SysBaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "role_id", type = IdType.AUTO)
    private Long roleId;

    private String roleName;
    private String roleKey;
    private Integer roleSort;
    private String dataScope;
    private String status;
    private String delFlag;

    @TableField(exist = false)
    private Long[] menuIds;

    @TableField(exist = false)
    private Long[] deptIds;

    @JsonIgnore
    public boolean isAdmin() {
        return Constants.SUPER_ADMIN.equals(roleKey);
    }
}
