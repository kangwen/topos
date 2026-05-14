package com.topos.admin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.topos.admin.common.core.domain.entity.SysMenu;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 菜单权限表；按用户解析权限标识（{@code perms}）。
 */
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    @Select("SELECT DISTINCT m.perms FROM sys_menu m "
            + "INNER JOIN sys_role_menu rm ON m.menu_id = rm.menu_id "
            + "INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id "
            + "WHERE ur.user_id = #{userId} AND m.perms IS NOT NULL AND m.perms <> ''")
    List<String> selectMenuPermsByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT menu_id, parent_id, menu_name, order_num, url, target, menu_type, visible, is_refresh,
                   perms, icon, create_by, create_time, update_by, update_time, remark
            FROM sys_menu
            WHERE menu_type IN ('M', 'C') AND visible = '0'
            ORDER BY parent_id, order_num
            """)
    List<SysMenu> selectMenuTreeAll();

    @Select("""
            SELECT DISTINCT m.menu_id, m.parent_id, m.menu_name, m.order_num, m.url, m.target, m.menu_type,
                   m.visible, m.is_refresh, m.perms, m.icon, m.create_by, m.create_time, m.update_by,
                   m.update_time, m.remark
            FROM sys_menu m
            INNER JOIN sys_role_menu rm ON m.menu_id = rm.menu_id
            INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id
            WHERE ur.user_id = #{userId} AND m.menu_type IN ('M', 'C') AND m.visible = '0'
            ORDER BY m.parent_id, m.order_num
            """)
    List<SysMenu> selectMenusByUserId(@Param("userId") Long userId);
}
