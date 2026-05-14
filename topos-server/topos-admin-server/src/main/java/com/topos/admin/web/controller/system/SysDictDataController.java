package com.topos.admin.web.controller.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.topos.admin.common.core.domain.AdminResult;
import com.topos.admin.common.core.domain.entity.SysDictData;
import com.topos.admin.common.core.page.toposPageSupport;
import com.topos.admin.common.core.page.TableDataInfo;
import com.topos.admin.system.mapper.SysDictDataMapper;
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

/**
 * 字典数据
 */
@RestController
@RequestMapping("/system/dict/data")
public class SysDictDataController {

    private final SysDictDataMapper sysDictDataMapper;

    public SysDictDataController(SysDictDataMapper sysDictDataMapper) {
        this.sysDictDataMapper = sysDictDataMapper;
    }

    @PreAuthorize("@ss.hasPermi('system:dict:list')")
    @GetMapping("/list")
    public TableDataInfo list(
            @RequestParam(required = false) String dictType,
            @RequestParam(required = false) String dictLabel,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        LambdaQueryWrapper<SysDictData> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(dictType)) {
            w.eq(SysDictData::getDictType, dictType);
        }
        if (StringUtils.hasText(dictLabel)) {
            w.like(SysDictData::getDictLabel, dictLabel);
        }
        if (StringUtils.hasText(status)) {
            w.eq(SysDictData::getStatus, status);
        }
        Page<SysDictData> page = sysDictDataMapper.selectPage(new Page<>(pageNum, pageSize),
                w.orderByAsc(SysDictData::getDictSort));
        return toposPageSupport.of(page);
    }

    @PreAuthorize("@ss.hasPermi('system:dict:query')")
    @GetMapping("/{dictCode}")
    public AdminResult get(@PathVariable Long dictCode) {
        return AdminResult.success(sysDictDataMapper.selectById(dictCode));
    }

    @GetMapping("/type/{dictType}")
    public AdminResult byType(@PathVariable String dictType) {
        List<SysDictData> list = sysDictDataMapper.selectList(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getStatus, "0")
                .orderByAsc(SysDictData::getDictSort));
        return AdminResult.success(list);
    }

    @PreAuthorize("@ss.hasPermi('system:dict:add')")
    @PostMapping
    public AdminResult add(@RequestBody SysDictData row) {
        sysDictDataMapper.insert(row);
        return AdminResult.success();
    }

    @PreAuthorize("@ss.hasPermi('system:dict:edit')")
    @PutMapping
    public AdminResult edit(@RequestBody SysDictData row) {
        sysDictDataMapper.updateById(row);
        return AdminResult.success();
    }

    @PreAuthorize("@ss.hasPermi('system:dict:remove')")
    @DeleteMapping("/{dictCodes}")
    public AdminResult remove(@PathVariable String dictCodes) {
        for (String id : dictCodes.split(",")) {
            if (StringUtils.hasText(id)) {
                sysDictDataMapper.deleteById(Long.parseLong(id.trim()));
            }
        }
        return AdminResult.success();
    }
}
