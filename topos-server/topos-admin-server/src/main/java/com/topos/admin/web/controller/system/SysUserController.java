package com.topos.admin.web.controller.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.topos.admin.common.core.domain.AdminResult;
import com.topos.admin.common.core.domain.entity.SysDept;
import com.topos.admin.common.core.domain.entity.SysRole;
import com.topos.admin.common.core.domain.entity.SysUser;
import com.topos.admin.common.core.domain.entity.SysUserPost;
import com.topos.admin.common.core.domain.entity.SysUserRole;
import com.topos.admin.common.core.domain.model.LoginUser;
import com.topos.admin.common.core.page.toposPageSupport;
import com.topos.admin.common.core.page.TableDataInfo;
import com.topos.admin.common.utils.SecurityUtils;
import com.topos.admin.system.mapper.SysDeptMapper;
import com.topos.admin.system.mapper.SysRoleMapper;
import com.topos.admin.system.mapper.SysUserMapper;
import com.topos.admin.system.mapper.SysUserPostMapper;
import com.topos.admin.system.mapper.SysUserRoleMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户管理
 */
@RestController
@RequestMapping("/system/user")
public class SysUserController {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SysUserMapper sysUserMapper;
    private final SysDeptMapper sysDeptMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysUserPostMapper sysUserPostMapper;
    private final PasswordEncoder passwordEncoder;

    public SysUserController(
            SysUserMapper sysUserMapper,
            SysDeptMapper sysDeptMapper,
            SysRoleMapper sysRoleMapper,
            SysUserRoleMapper sysUserRoleMapper,
            SysUserPostMapper sysUserPostMapper,
            PasswordEncoder passwordEncoder) {
        this.sysUserMapper = sysUserMapper;
        this.sysDeptMapper = sysDeptMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.sysUserPostMapper = sysUserPostMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @PreAuthorize("@ss.hasPermi('system:user:list')")
    @GetMapping("/list")
    public TableDataInfo list(
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String phonenumber,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String beginTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<SysUser>().eq(SysUser::getDelFlag, "0");
        if (StringUtils.hasText(userName)) {
            w.like(SysUser::getLoginName, userName);
        }
        if (StringUtils.hasText(phonenumber)) {
            w.like(SysUser::getPhonenumber, phonenumber);
        }
        if (StringUtils.hasText(status)) {
            w.eq(SysUser::getStatus, status);
        }
        if (deptId != null) {
            w.eq(SysUser::getDeptId, deptId);
        }
        if (StringUtils.hasText(beginTime)) {
            w.ge(SysUser::getCreateTime, LocalDateTime.parse(beginTime.trim(), DT));
        }
        if (StringUtils.hasText(endTime)) {
            w.le(SysUser::getCreateTime, LocalDateTime.parse(endTime.trim(), DT));
        }
        Page<SysUser> page = sysUserMapper.selectPage(new Page<>(pageNum, pageSize), w.orderByDesc(SysUser::getUserId));
        for (SysUser u : page.getRecords()) {
            if (u.getDeptId() != null) {
                SysDept d = sysDeptMapper.selectById(u.getDeptId());
                u.setDept(d);
            }
        }
        return toposPageSupport.of(page);
    }

    @PreAuthorize("@ss.hasPermi('system:user:query')")
    @GetMapping("/{userId}")
    public AdminResult get(@PathVariable Long userId) {
        SysUser u = sysUserMapper.selectById(userId);
        if (u != null) {
            u.setRoles(sysRoleMapper.selectRolesByUserId(userId));
            if (u.getDeptId() != null) {
                u.setDept(sysDeptMapper.selectById(u.getDeptId()));
            }
        }
        List<Long> roleIds = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId)).stream()
                .map(SysUserRole::getRoleId).collect(Collectors.toList());
        List<Long> postIds = sysUserPostMapper.selectList(new LambdaQueryWrapper<SysUserPost>()
                        .eq(SysUserPost::getUserId, userId)).stream()
                .map(SysUserPost::getPostId).collect(Collectors.toList());
        AdminResult r = AdminResult.success(u);
        r.put("roleIds", roleIds);
        r.put("postIds", postIds);
        return r;
    }

    @PreAuthorize("@ss.hasPermi('system:user:add')")
    @PostMapping
    public AdminResult add(@RequestBody SysUser user) {
        user.setDelFlag("0");
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        sysUserMapper.insert(user);
        saveUserRoles(user.getUserId(), user.getRoleIds());
        saveUserPosts(user.getUserId(), user.getPostIds());
        return AdminResult.success();
    }

    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @PutMapping
    public AdminResult edit(@RequestBody SysUser user) {
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(null);
        }
        sysUserMapper.updateById(user);
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getUserId()));
        sysUserPostMapper.delete(new LambdaQueryWrapper<SysUserPost>().eq(SysUserPost::getUserId, user.getUserId()));
        saveUserRoles(user.getUserId(), user.getRoleIds());
        saveUserPosts(user.getUserId(), user.getPostIds());
        return AdminResult.success();
    }

    @PreAuthorize("@ss.hasPermi('system:user:remove')")
    @DeleteMapping("/{userIds}")
    public AdminResult remove(@PathVariable String userIds) {
        for (String id : userIds.split(",")) {
            Long uid = Long.parseLong(id.trim());
            sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, uid));
            sysUserPostMapper.delete(new LambdaQueryWrapper<SysUserPost>().eq(SysUserPost::getUserId, uid));
            SysUser del = new SysUser();
            del.setUserId(uid);
            del.setDelFlag("2");
            sysUserMapper.updateById(del);
        }
        return AdminResult.success();
    }

    @PreAuthorize("@ss.hasPermi('system:user:resetPwd')")
    @PutMapping("/resetPwd")
    public AdminResult resetPwd(@RequestBody SysUser body) {
        SysUser u = new SysUser();
        u.setUserId(body.getUserId());
        u.setPassword(passwordEncoder.encode(body.getPassword()));
        sysUserMapper.updateById(u);
        return AdminResult.success();
    }

    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @PutMapping("/changeStatus")
    public AdminResult changeStatus(@RequestBody SysUser body) {
        SysUser u = new SysUser();
        u.setUserId(body.getUserId());
        u.setStatus(body.getStatus());
        sysUserMapper.updateById(u);
        return AdminResult.success();
    }

    @GetMapping("/profile")
    public AdminResult profile() {
        LoginUser lu = SecurityUtils.getLoginUser();
        SysUser u = lu.getUser();
        if (u != null && u.getUserId() != null) {
            u = sysUserMapper.selectById(u.getUserId());
            u.setRoles(sysRoleMapper.selectRolesByUserId(u.getUserId()));
        }
        return AdminResult.success(u);
    }

    @PutMapping("/profile")
    public AdminResult updateProfile(@RequestBody SysUser user) {
        LoginUser lu = SecurityUtils.getLoginUser();
        SysUser db = sysUserMapper.selectById(lu.getUserId());
        if (db == null) {
            return AdminResult.error("用户不存在");
        }
        db.setNickName(user.getNickName());
        db.setEmail(user.getEmail());
        db.setPhonenumber(user.getPhonenumber());
        db.setSex(user.getSex());
        sysUserMapper.updateById(db);
        return AdminResult.success();
    }

    @PutMapping("/profile/updatePwd")
    public AdminResult updatePwd(@RequestBody Map<String, String> body) {
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        LoginUser lu = SecurityUtils.getLoginUser();
        SysUser db = sysUserMapper.selectById(lu.getUserId());
        if (db == null || !passwordEncoder.matches(oldPassword, db.getPassword())) {
            return AdminResult.error("修改密码失败，旧密码错误");
        }
        db.setPassword(passwordEncoder.encode(newPassword));
        sysUserMapper.updateById(db);
        return AdminResult.success();
    }

    @PostMapping("/profile/avatar")
    public AdminResult avatar() {
        return AdminResult.warn("头像上传未实现");
    }

    @PreAuthorize("@ss.hasPermi('system:user:query')")
    @GetMapping("/authRole/{userId}")
    public AdminResult authRole(@PathVariable Long userId) {
        SysUser u = sysUserMapper.selectById(userId);
        List<SysRole> roles = sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>().eq(SysRole::getDelFlag, "0"));
        List<Long> checked = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId)).stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
        AdminResult r = AdminResult.success();
        r.put("user", u);
        r.put("roles", roles);
        r.put("roleIds", checked);
        return r;
    }

    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @PutMapping("/authRole")
    public AdminResult updateAuthRole(@RequestParam Long userId, @RequestParam(required = false) String roleIds) {
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (StringUtils.hasText(roleIds)) {
            for (String s : roleIds.split(",")) {
                if (!StringUtils.hasText(s)) {
                    continue;
                }
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(Long.parseLong(s.trim()));
                sysUserRoleMapper.insert(ur);
            }
        }
        return AdminResult.success();
    }

    @PreAuthorize("@ss.hasPermi('system:user:list')")
    @GetMapping("/deptTree")
    public AdminResult deptTree() {
        List<SysDept> depts = sysDeptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getDelFlag, "0")
                .orderByAsc(SysDept::getParentId, SysDept::getOrderNum));
        return AdminResult.success(SysDeptController.toDeptTreeSelect(depts));
    }

    @PostMapping("/importData")
    public AdminResult importData() {
        return AdminResult.warn("导入未实现");
    }

    private void saveUserRoles(Long userId, Long[] roleIds) {
        if (userId == null || roleIds == null) {
            return;
        }
        for (Long rid : roleIds) {
            if (rid == null) {
                continue;
            }
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(rid);
            sysUserRoleMapper.insert(ur);
        }
    }

    private void saveUserPosts(Long userId, Long[] postIds) {
        if (userId == null || postIds == null) {
            return;
        }
        for (Long pid : postIds) {
            if (pid == null) {
                continue;
            }
            SysUserPost up = new SysUserPost();
            up.setUserId(userId);
            up.setPostId(pid);
            sysUserPostMapper.insert(up);
        }
    }
}
