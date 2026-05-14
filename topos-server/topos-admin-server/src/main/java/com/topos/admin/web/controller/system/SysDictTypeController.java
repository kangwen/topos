package com.topos.admin.web.controller.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.topos.admin.common.core.domain.AdminResult;
import com.topos.admin.common.core.domain.entity.SysDictType;
import com.topos.admin.common.core.page.toposPageSupport;
import com.topos.admin.common.core.page.TableDataInfo;
import com.topos.admin.system.mapper.SysDictTypeMapper;
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
 * 字典类型
 */
@RestController
@RequestMapping("/system/dict/type")
public class SysDictTypeController {

    private final SysDictTypeMapper sysDictTypeMapper;

    public SysDictTypeController(SysDictTypeMapper sysDictTypeMapper) {
        this.sysDictTypeMapper = sysDictTypeMapper;
    }

    @PreAuthorize("@ss.hasPermi('system:dict:list')")
    @GetMapping("/list")
    public TableDataInfo list(
            @RequestParam(required = false) String dictName,
            @RequestParam(required = false) String dictType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        LambdaQueryWrapper<SysDictType> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(dictName)) {
            w.like(SysDictType::getDictName, dictName);
        }
        if (StringUtils.hasText(dictType)) {
            w.like(SysDictType::getDictType, dictType);
        }
        if (StringUtils.hasText(status)) {
            w.eq(SysDictType::getStatus, status);
        }
        Page<SysDictType> page = sysDictTypeMapper.selectPage(new Page<>(pageNum, pageSize), w.orderByAsc(SysDictType::getDictId));
        return toposPageSupport.of(page);
    }

    @PreAuthorize("@ss.hasPermi('system:dict:query')")
    @GetMapping("/{dictId}")
    public AdminResult get(@PathVariable Long dictId) {
        return AdminResult.success(sysDictTypeMapper.selectById(dictId));
    }

    @PreAuthorize("@ss.hasPermi('system:dict:add')")
    @PostMapping
    public AdminResult add(@RequestBody SysDictType row) {
        sysDictTypeMapper.insert(row);
        return AdminResult.success();
    }

    @PreAuthorize("@ss.hasPermi('system:dict:edit')")
    @PutMapping
    public AdminResult edit(@RequestBody SysDictType row) {
        sysDictTypeMapper.updateById(row);
        return AdminResult.success();
    }

    @PreAuthorize("@ss.hasPermi('system:dict:remove')")
    @DeleteMapping("/{dictIds}")
    public AdminResult remove(@PathVariable String dictIds) {
        for (String id : dictIds.split(",")) {
            if (StringUtils.hasText(id)) {
                sysDictTypeMapper.deleteById(Long.parseLong(id.trim()));
            }
        }
        return AdminResult.success();
    }
}
