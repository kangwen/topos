package com.topos.admin.web.controller.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.topos.admin.common.core.domain.AdminResult;
import com.topos.admin.common.core.domain.entity.SysNotice;
import com.topos.admin.common.core.page.ToposPageSupport;
import com.topos.admin.common.core.page.TableDataInfo;
import com.topos.admin.system.mapper.SysNoticeMapper;
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
 * 通知公告
 */
@RestController
@RequestMapping("/system/notice")
public class SysNoticeController {

    private final SysNoticeMapper sysNoticeMapper;

    public SysNoticeController(SysNoticeMapper sysNoticeMapper) {
        this.sysNoticeMapper = sysNoticeMapper;
    }

    @PreAuthorize("@ss.hasPermi('system:notice:list')")
    @GetMapping("/list")
    public TableDataInfo list(
            @RequestParam(required = false) String noticeTitle,
            @RequestParam(required = false) String createBy,
            @RequestParam(required = false) String noticeType,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        LambdaQueryWrapper<SysNotice> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(noticeTitle)) {
            w.like(SysNotice::getNoticeTitle, noticeTitle);
        }
        if (StringUtils.hasText(createBy)) {
            w.eq(SysNotice::getCreateBy, createBy);
        }
        if (StringUtils.hasText(noticeType)) {
            w.eq(SysNotice::getNoticeType, noticeType);
        }
        Page<SysNotice> page = sysNoticeMapper.selectPage(new Page<>(pageNum, pageSize), w.orderByDesc(SysNotice::getNoticeId));
        return ToposPageSupport.of(page);
    }

    @PreAuthorize("@ss.hasPermi('system:notice:query')")
    @GetMapping("/{noticeId}")
    public AdminResult get(@PathVariable Integer noticeId) {
        return AdminResult.success(sysNoticeMapper.selectById(noticeId));
    }

    @PreAuthorize("@ss.hasPermi('system:notice:add')")
    @PostMapping
    public AdminResult add(@RequestBody SysNotice notice) {
        sysNoticeMapper.insert(notice);
        return AdminResult.success();
    }

    @PreAuthorize("@ss.hasPermi('system:notice:edit')")
    @PutMapping
    public AdminResult edit(@RequestBody SysNotice notice) {
        sysNoticeMapper.updateById(notice);
        return AdminResult.success();
    }

    @PreAuthorize("@ss.hasPermi('system:notice:remove')")
    @DeleteMapping("/{noticeIds}")
    public AdminResult remove(@PathVariable String noticeIds) {
        for (String id : noticeIds.split(",")) {
            if (StringUtils.hasText(id)) {
                sysNoticeMapper.deleteById(Integer.parseInt(id.trim()));
            }
        }
        return AdminResult.success();
    }
}
