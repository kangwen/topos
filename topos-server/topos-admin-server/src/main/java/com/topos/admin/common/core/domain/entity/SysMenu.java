package com.topos.admin.common.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 菜单权限表 {@code sys_menu}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends SysBaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "menu_id", type = IdType.AUTO)
    private Long menuId;

    private String menuName;
    private Long parentId;
    private Integer orderNum;

    /** 表字段 url；JSON 与前端字段名 path 对齐 */
    @JsonProperty("path")
    private String url;
    private String target;
    private String menuType;
    private String visible;
    private String isRefresh;
    private String perms;
    private String icon;

    /** 前端展示/编辑用，表内无此列 */
    @TableField(exist = false)
    private String component;

    @TableField(exist = false)
    private String routeName;

    @TableField(exist = false)
    private String query;

    @TableField(exist = false)
    private String status;

    @TableField(exist = false)
    private String isFrame;

    @TableField(exist = false)
    private String isCache;

    @TableField(exist = false)
    private List<SysMenu> children;
}
