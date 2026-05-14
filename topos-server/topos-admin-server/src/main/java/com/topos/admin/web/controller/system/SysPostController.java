package com.topos.admin.web.controller.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.topos.admin.common.core.domain.AdminResult;
import com.topos.admin.common.core.domain.entity.SysPost;
import com.topos.admin.common.core.page.toposPageSupport;
import com.topos.admin.common.core.page.TableDataInfo;
import com.topos.admin.system.mapper.SysPostMapper;
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

/**
 * 岗位管理
 */
@RestController
@RequestMapping("/system/post")
public class SysPostController {

    private final SysPostMapper sysPostMapper;

    public SysPostController(SysPostMapper sysPostMapper) {
        this.sysPostMapper = sysPostMapper;
    }

    @PreAuthorize("@ss.hasPermi('system:post:list')")
    @GetMapping("/list")
    public TableDataInfo list(
            @RequestParam(required = false) String postCode,
            @RequestParam(required = false) String postName,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        LambdaQueryWrapper<SysPost> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(postCode)) {
            w.like(SysPost::getPostCode, postCode);
        }
        if (StringUtils.hasText(postName)) {
            w.like(SysPost::getPostName, postName);
        }
        if (StringUtils.hasText(status)) {
            w.eq(SysPost::getStatus, status);
        }
        Page<SysPost> page = sysPostMapper.selectPage(new Page<>(pageNum, pageSize), w.orderByAsc(SysPost::getPostSort));
        return toposPageSupport.of(page);
    }

    @PreAuthorize("@ss.hasPermi('system:post:query')")
    @GetMapping("/{postId}")
    public AdminResult get(@PathVariable Long postId) {
        return AdminResult.success(sysPostMapper.selectById(postId));
    }

    @PreAuthorize("@ss.hasPermi('system:post:add')")
    @PostMapping
    public AdminResult add(@RequestBody SysPost post) {
        sysPostMapper.insert(post);
        return AdminResult.success();
    }

    @PreAuthorize("@ss.hasPermi('system:post:edit')")
    @PutMapping
    public AdminResult edit(@RequestBody SysPost post) {
        sysPostMapper.updateById(post);
        return AdminResult.success();
    }

    @PreAuthorize("@ss.hasPermi('system:post:remove')")
    @DeleteMapping("/{postIds}")
    public AdminResult remove(@PathVariable String postIds) {
        for (String id : postIds.split(",")) {
            if (StringUtils.hasText(id)) {
                sysPostMapper.deleteById(Long.parseLong(id.trim()));
            }
        }
        return AdminResult.success();
    }
}
