package com.topos.admin.web.controller.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.topos.admin.common.core.domain.AdminResult;
import com.topos.admin.common.core.domain.entity.SysDept;
import com.topos.admin.system.mapper.SysDeptMapper;
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 部门管理
 */
@RestController
@RequestMapping("/system/dept")
public class SysDeptController {

    private final SysDeptMapper sysDeptMapper;

    public SysDeptController(SysDeptMapper sysDeptMapper) {
        this.sysDeptMapper = sysDeptMapper;
    }

    @PreAuthorize("@ss.hasPermi('system:dept:list')")
    @GetMapping("/list")
    public AdminResult list(SysDept query) {
        LambdaQueryWrapper<SysDept> w = new LambdaQueryWrapper<SysDept>().eq(SysDept::getDelFlag, "0");
        if (query != null) {
            if (StringUtils.hasText(query.getDeptName())) {
                w.like(SysDept::getDeptName, query.getDeptName());
            }
            if (StringUtils.hasText(query.getStatus())) {
                w.eq(SysDept::getStatus, query.getStatus());
            }
        }
        w.orderByAsc(SysDept::getParentId, SysDept::getOrderNum);
        return AdminResult.success(sysDeptMapper.selectList(w));
    }

    @PreAuthorize("@ss.hasPermi('system:dept:list')")
    @GetMapping("/list/exclude/{deptId}")
    public AdminResult exclude(@PathVariable Long deptId) {
        List<SysDept> all = listAllNormal();
        Set<Long> ban = new HashSet<>();
        ban.add(deptId);
        ban.addAll(descendantIds(deptId, all));
        List<SysDept> out = all.stream().filter(d -> !ban.contains(d.getDeptId())).collect(Collectors.toList());
        return AdminResult.success(out);
    }

    @PreAuthorize("@ss.hasPermi('system:dept:query')")
    @GetMapping("/{deptId}")
    public AdminResult get(@PathVariable Long deptId) {
        return AdminResult.success(sysDeptMapper.selectById(deptId));
    }

    @PreAuthorize("@ss.hasPermi('system:dept:add')")
    @PostMapping
    public AdminResult add(@RequestBody SysDept dept) {
        fillAncestors(dept);
        dept.setDelFlag("0");
        sysDeptMapper.insert(dept);
        return AdminResult.success();
    }

    @PreAuthorize("@ss.hasPermi('system:dept:edit')")
    @PutMapping
    public AdminResult edit(@RequestBody SysDept dept) {
        fillAncestors(dept);
        sysDeptMapper.updateById(dept);
        return AdminResult.success();
    }

    @PreAuthorize("@ss.hasPermi('system:dept:remove')")
    @DeleteMapping("/{deptId}")
    public AdminResult remove(@PathVariable Long deptId) {
        sysDeptMapper.deleteById(deptId);
        return AdminResult.success();
    }

    private List<SysDept> listAllNormal() {
        return sysDeptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getDelFlag, "0")
                .orderByAsc(SysDept::getParentId, SysDept::getOrderNum));
    }

    private void fillAncestors(SysDept dept) {
        if (dept.getParentId() == null || dept.getParentId() == 0L) {
            dept.setAncestors("0");
            return;
        }
        SysDept parent = sysDeptMapper.selectById(dept.getParentId());
        if (parent != null && StringUtils.hasText(parent.getAncestors())) {
            dept.setAncestors(parent.getAncestors() + "," + parent.getDeptId());
        } else if (parent != null) {
            dept.setAncestors("0," + parent.getDeptId());
        } else {
            dept.setAncestors("0");
        }
    }

    private static Set<Long> descendantIds(Long rootId, List<SysDept> all) {
        Set<Long> out = new HashSet<>();
        Deque<Long> q = new ArrayDeque<>();
        q.add(rootId);
        while (!q.isEmpty()) {
            Long cur = q.poll();
            for (SysDept d : all) {
                if (Objects.equals(d.getParentId(), cur)) {
                    out.add(d.getDeptId());
                    q.add(d.getDeptId());
                }
            }
        }
        return out;
    }

    static List<TreeSelect> toDeptTreeSelect(List<SysDept> depts) {
        List<TreeSelect> roots = new ArrayList<>();
        for (SysDept d : depts) {
            if (d.getParentId() == null || d.getParentId() == 0L) {
                roots.add(buildDeptNode(d, depts));
            }
        }
        return roots;
    }

    private static TreeSelect buildDeptNode(SysDept d, List<SysDept> all) {
        boolean disabled = !"0".equals(d.getStatus());
        TreeSelect n = new TreeSelect(d.getDeptId(), d.getDeptName(), disabled);
        for (SysDept c : all) {
            if (Objects.equals(c.getParentId(), d.getDeptId())) {
                n.getChildren().add(buildDeptNode(c, all));
            }
        }
        return n;
    }
}
