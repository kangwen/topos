package com.topos.admin.web.controller.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.topos.admin.common.core.domain.AdminResult;
import com.topos.admin.common.core.domain.entity.SysMenu;
import com.topos.admin.common.core.domain.entity.SysRoleMenu;
import com.topos.admin.system.mapper.SysMenuMapper;
import com.topos.admin.system.mapper.SysRoleMenuMapper;
import com.topos.admin.system.support.SysMenuDisplayHelper;
import com.topos.admin.web.domain.vo.TreeSelect;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 菜单管理（CRUD，与动态路由 {@link com.topos.admin.framework.web.service.AdminMenuService} 分离）。
 */
@RestController
@RequestMapping("/system/menu")
public class SysMenuCrudController {

    private final SysMenuMapper sysMenuMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;

    public SysMenuCrudController(SysMenuMapper sysMenuMapper, SysRoleMenuMapper sysRoleMenuMapper) {
        this.sysMenuMapper = sysMenuMapper;
        this.sysRoleMenuMapper = sysRoleMenuMapper;
    }

    @PreAuthorize("@ss.hasPermi('system:menu:list')")
    @GetMapping("/list")
    public AdminResult list(SysMenu query) {
        LambdaQueryWrapper<SysMenu> w = new LambdaQueryWrapper<>();
        if (query != null) {
            if (StringUtils.hasText(query.getMenuName())) {
                w.like(SysMenu::getMenuName, query.getMenuName());
            }
            if (StringUtils.hasText(query.getVisible())) {
                w.eq(SysMenu::getVisible, query.getVisible());
            }
        }
        w.orderByAsc(SysMenu::getParentId, SysMenu::getOrderNum);
        List<SysMenu> list = sysMenuMapper.selectList(w);
        List<SysMenu> tree = buildTree(list, 0L);
        SysMenuDisplayHelper.enrichTree(tree);
        return AdminResult.success(tree);
    }

    @PreAuthorize("@ss.hasPermi('system:menu:query')")
    @GetMapping("/{menuId}")
    public AdminResult get(@PathVariable Long menuId) {
        SysMenu m = sysMenuMapper.selectById(menuId);
        SysMenuDisplayHelper.enrich(m);
        return AdminResult.success(m);
    }

    @PreAuthorize("@ss.hasPermi('system:menu:add')")
    @PostMapping
    public AdminResult add(@RequestBody SysMenu menu) {
        sysMenuMapper.insert(menu);
        return AdminResult.success();
    }

    @PreAuthorize("@ss.hasPermi('system:menu:edit')")
    @PutMapping
    public AdminResult edit(@RequestBody SysMenu menu) {
        sysMenuMapper.updateById(menu);
        return AdminResult.success();
    }

    @PreAuthorize("@ss.hasPermi('system:menu:remove')")
    @DeleteMapping("/{menuId}")
    public AdminResult remove(@PathVariable Long menuId) {
        sysMenuMapper.deleteById(menuId);
        return AdminResult.success();
    }

    @GetMapping("/treeselect")
    public AdminResult treeselect() {
        List<SysMenu> menus = sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .in(SysMenu::getMenuType, "M", "C")
                .orderByAsc(SysMenu::getParentId, SysMenu::getOrderNum));
        return AdminResult.success(toMenuTreeSelect(menus));
    }

    @GetMapping("/roleMenuTreeselect/{roleId}")
    public AdminResult roleMenuTreeselect(@PathVariable Long roleId) {
        List<SysMenu> menus = sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .in(SysMenu::getMenuType, "M", "C")
                .orderByAsc(SysMenu::getParentId, SysMenu::getOrderNum));
        List<Long> checked = sysRoleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId))
                .stream().map(SysRoleMenu::getMenuId).collect(Collectors.toList());
        AdminResult r = AdminResult.success();
        r.put("menus", toMenuTreeSelect(menus));
        r.put("checkedKeys", checked);
        return r;
    }

    private static List<SysMenu> buildTree(List<SysMenu> flat, long parentId) {
        List<SysMenu> roots = new ArrayList<>();
        for (SysMenu m : flat) {
            long pid = m.getParentId() == null ? 0L : m.getParentId();
            if (pid == parentId) {
                m.setChildren(buildTree(flat, m.getMenuId()));
                roots.add(m);
            }
        }
        return roots;
    }

    private static List<TreeSelect> toMenuTreeSelect(List<SysMenu> menus) {
        List<TreeSelect> roots = new ArrayList<>();
        for (SysMenu m : menus) {
            long pid = m.getParentId() == null ? 0L : m.getParentId();
            if (pid == 0L) {
                roots.add(buildMenuNode(m, menus));
            }
        }
        return roots;
    }

    private static TreeSelect buildMenuNode(SysMenu m, List<SysMenu> all) {
        TreeSelect n = new TreeSelect(m.getMenuId(), m.getMenuName(), false);
        for (SysMenu c : all) {
            if (Objects.equals(c.getParentId(), m.getMenuId())) {
                n.getChildren().add(buildMenuNode(c, all));
            }
        }
        return n;
    }
}
