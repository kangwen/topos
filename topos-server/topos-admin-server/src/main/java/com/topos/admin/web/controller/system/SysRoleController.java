package com.topos.admin.web.controller.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.topos.admin.common.core.domain.AdminResult;
import com.topos.admin.common.core.domain.entity.SysDept;
import com.topos.admin.common.core.domain.entity.SysRole;
import com.topos.admin.common.core.domain.entity.SysRoleDept;
import com.topos.admin.common.core.domain.entity.SysRoleMenu;
import com.topos.admin.common.core.domain.entity.SysUser;
import com.topos.admin.common.core.domain.entity.SysUserRole;
import com.topos.admin.common.core.page.ToposPageSupport;
import com.topos.admin.common.core.page.TableDataInfo;
import com.topos.admin.system.mapper.SysDeptMapper;
import com.topos.admin.system.mapper.SysRoleDeptMapper;
import com.topos.admin.system.mapper.SysRoleMapper;
import com.topos.admin.system.mapper.SysRoleMenuMapper;
import com.topos.admin.system.mapper.SysUserMapper;
import com.topos.admin.system.mapper.SysUserRoleMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色管理
 */
@RestController
@RequestMapping("/system/role")
public class SysRoleController {

    private final SysRoleMapper sysRoleMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SysRoleDeptMapper sysRoleDeptMapper;
    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysDeptMapper sysDeptMapper;

    public SysRoleController(
            SysRoleMapper sysRoleMapper,
            SysRoleMenuMapper sysRoleMenuMapper,
            SysRoleDeptMapper sysRoleDeptMapper,
            SysUserMapper sysUserMapper,
            SysUserRoleMapper sysUserRoleMapper,
            SysDeptMapper sysDeptMapper) {
        this.sysRoleMapper = sysRoleMapper;
        this.sysRoleMenuMapper = sysRoleMenuMapper;
        this.sysRoleDeptMapper = sysRoleDeptMapper;
        this.sysUserMapper = sysUserMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.sysDeptMapper = sysDeptMapper;
    }

    @PreAuthorize("@ss.hasPermi('system:role:list')")
    @GetMapping("/list")
    public TableDataInfo list(
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) String roleKey,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        LambdaQueryWrapper<SysRole> w = new LambdaQueryWrapper<SysRole>().eq(SysRole::getDelFlag, "0");
        if (StringUtils.hasText(roleName)) {
            w.like(SysRole::getRoleName, roleName);
        }
        if (StringUtils.hasText(roleKey)) {
            w.like(SysRole::getRoleKey, roleKey);
        }
        if (StringUtils.hasText(status)) {
            w.eq(SysRole::getStatus, status);
        }
        Page<SysRole> page = sysRoleMapper.selectPage(new Page<>(pageNum, pageSize), w.orderByAsc(SysRole::getRoleSort));
        return ToposPageSupport.of(page);
    }

    @PreAuthorize("@ss.hasPermi('system:role:query')")
    @GetMapping("/{roleId}")
    public AdminResult get(@PathVariable Long roleId) {
        SysRole role = sysRoleMapper.selectById(roleId);
        if (role != null) {
            List<Long> mids = sysRoleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId))
                    .stream().map(SysRoleMenu::getMenuId).collect(Collectors.toList());
            role.setMenuIds(mids.toArray(new Long[0]));
        }
        return AdminResult.success(role);
    }

    @PreAuthorize("@ss.hasPermi('system:role:add')")
    @PostMapping
    public AdminResult add(@RequestBody SysRole role) {
        role.setDelFlag("0");
        sysRoleMapper.insert(role);
        saveRoleMenus(role.getRoleId(), role.getMenuIds());
        return AdminResult.success();
    }

    @PreAuthorize("@ss.hasPermi('system:role:edit')")
    @PutMapping
    public AdminResult edit(@RequestBody SysRole role) {
        sysRoleMapper.updateById(role);
        sysRoleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, role.getRoleId()));
        saveRoleMenus(role.getRoleId(), role.getMenuIds());
        return AdminResult.success();
    }

    @PreAuthorize("@ss.hasPermi('system:role:edit')")
    @PutMapping("/dataScope")
    public AdminResult dataScope(@RequestBody SysRole role) {
        SysRole patch = new SysRole();
        patch.setRoleId(role.getRoleId());
        patch.setDataScope(role.getDataScope());
        sysRoleMapper.updateById(patch);
        sysRoleDeptMapper.delete(new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, role.getRoleId()));
        if (role.getDeptIds() != null) {
            for (Long did : role.getDeptIds()) {
                if (did == null) {
                    continue;
                }
                SysRoleDept rd = new SysRoleDept();
                rd.setRoleId(role.getRoleId());
                rd.setDeptId(did);
                sysRoleDeptMapper.insert(rd);
            }
        }
        return AdminResult.success();
    }

    @PreAuthorize("@ss.hasPermi('system:role:edit')")
    @PutMapping("/changeStatus")
    public AdminResult changeStatus(@RequestBody SysRole body) {
        SysRole r = new SysRole();
        r.setRoleId(body.getRoleId());
        r.setStatus(body.getStatus());
        sysRoleMapper.updateById(r);
        return AdminResult.success();
    }

    @PreAuthorize("@ss.hasPermi('system:role:remove')")
    @DeleteMapping("/{roleIds}")
    public AdminResult remove(@PathVariable String roleIds) {
        for (String id : roleIds.split(",")) {
            Long rid = Long.parseLong(id.trim());
            sysRoleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, rid));
            SysRole r = new SysRole();
            r.setRoleId(rid);
            r.setDelFlag("2");
            sysRoleMapper.updateById(r);
        }
        return AdminResult.success();
    }

    @PreAuthorize("@ss.hasPermi('system:role:list')")
    @GetMapping("/authUser/allocatedList")
    public TableDataInfo allocatedList(
            @RequestParam Long roleId,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String phonenumber,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Set<Long> userIds = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, roleId))
                .stream().map(SysUserRole::getUserId).collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return ToposPageSupport.of(new Page<SysUser>(pageNum, pageSize, 0));
        }
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<SysUser>().in(SysUser::getUserId, userIds).eq(SysUser::getDelFlag, "0");
        if (StringUtils.hasText(userName)) {
            w.like(SysUser::getLoginName, userName);
        }
        if (StringUtils.hasText(phonenumber)) {
            w.like(SysUser::getPhonenumber, phonenumber);
        }
        Page<SysUser> page = sysUserMapper.selectPage(new Page<>(pageNum, pageSize), w);
        return ToposPageSupport.of(page);
    }

    @PreAuthorize("@ss.hasPermi('system:role:list')")
    @GetMapping("/authUser/unallocatedList")
    public TableDataInfo unallocatedList(
            @RequestParam Long roleId,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String phonenumber,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Set<Long> allocated = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, roleId))
                .stream().map(SysUserRole::getUserId).collect(Collectors.toSet());
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<SysUser>().eq(SysUser::getDelFlag, "0");
        if (!allocated.isEmpty()) {
            w.notIn(SysUser::getUserId, allocated);
        }
        if (StringUtils.hasText(userName)) {
            w.like(SysUser::getLoginName, userName);
        }
        if (StringUtils.hasText(phonenumber)) {
            w.like(SysUser::getPhonenumber, phonenumber);
        }
        Page<SysUser> page = sysUserMapper.selectPage(new Page<>(pageNum, pageSize), w);
        return ToposPageSupport.of(page);
    }

    @PreAuthorize("@ss.hasPermi('system:role:edit')")
    @PutMapping("/authUser/cancel")
    public AdminResult authUserCancel(@RequestBody SysUserRole ur) {
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, ur.getUserId())
                .eq(SysUserRole::getRoleId, ur.getRoleId()));
        return AdminResult.success();
    }

    @PreAuthorize("@ss.hasPermi('system:role:edit')")
    @PutMapping("/authUser/cancelAll")
    public AdminResult authUserCancelAll(@RequestParam Long roleId, @RequestParam String userIds) {
        for (String uid : userIds.split(",")) {
            if (!StringUtils.hasText(uid)) {
                continue;
            }
            sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
                    .eq(SysUserRole::getRoleId, roleId)
                    .eq(SysUserRole::getUserId, Long.parseLong(uid.trim())));
        }
        return AdminResult.success();
    }

    @PreAuthorize("@ss.hasPermi('system:role:edit')")
    @PutMapping("/authUser/selectAll")
    public AdminResult authUserSelectAll(@RequestParam Long roleId, @RequestParam String userIds) {
        for (String uid : userIds.split(",")) {
            if (!StringUtils.hasText(uid)) {
                continue;
            }
            long userId = Long.parseLong(uid.trim());
            long c = sysUserRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>()
                    .eq(SysUserRole::getUserId, userId)
                    .eq(SysUserRole::getRoleId, roleId));
            if (c == 0) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                sysUserRoleMapper.insert(ur);
            }
        }
        return AdminResult.success();
    }

    @PreAuthorize("@ss.hasPermi('system:role:query')")
    @GetMapping("/deptTree/{roleId}")
    public AdminResult deptTree(@PathVariable Long roleId) {
        List<SysDept> depts = sysDeptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getDelFlag, "0")
                .orderByAsc(SysDept::getParentId, SysDept::getOrderNum));
        List<Long> checked = sysRoleDeptMapper.selectList(new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, roleId))
                .stream().map(SysRoleDept::getDeptId).collect(Collectors.toList());
        AdminResult r = AdminResult.success();
        r.put("depts", SysDeptController.toDeptTreeSelect(depts));
        r.put("checkedKeys", checked);
        return r;
    }

    private void saveRoleMenus(Long roleId, Long[] menuIds) {
        if (roleId == null || menuIds == null) {
            return;
        }
        for (Long mid : menuIds) {
            if (mid == null) {
                continue;
            }
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(mid);
            sysRoleMenuMapper.insert(rm);
        }
    }
}
