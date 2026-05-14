package com.topos.admin.web.controller.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.topos.admin.common.core.domain.AdminResult;
import com.topos.admin.common.core.domain.entity.SysConfig;
import com.topos.admin.common.core.page.ToposPageSupport;
import com.topos.admin.common.core.page.TableDataInfo;
import com.topos.admin.system.mapper.SysConfigMapper;
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
 * 参数配置
 */
@RestController
@RequestMapping("/system/config")
public class SysConfigController {

    private final SysConfigMapper sysConfigMapper;

    public SysConfigController(SysConfigMapper sysConfigMapper) {
        this.sysConfigMapper = sysConfigMapper;
    }

    @PreAuthorize("@ss.hasPermi('system:config:list')")
    @GetMapping("/list")
    public TableDataInfo list(
            @RequestParam(required = false) String configName,
            @RequestParam(required = false) String configKey,
            @RequestParam(required = false) String configType,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        LambdaQueryWrapper<SysConfig> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(configName)) {
            w.like(SysConfig::getConfigName, configName);
        }
        if (StringUtils.hasText(configKey)) {
            w.like(SysConfig::getConfigKey, configKey);
        }
        if (StringUtils.hasText(configType)) {
            w.eq(SysConfig::getConfigType, configType);
        }
        Page<SysConfig> page = sysConfigMapper.selectPage(new Page<>(pageNum, pageSize), w.orderByAsc(SysConfig::getConfigId));
        return ToposPageSupport.of(page);
    }

    @PreAuthorize("@ss.hasPermi('system:config:query')")
    @GetMapping("/{configId}")
    public AdminResult get(@PathVariable Integer configId) {
        return AdminResult.success(sysConfigMapper.selectById(configId));
    }

    @GetMapping("/configKey/{configKey}")
    public AdminResult getByKey(@PathVariable String configKey) {
        SysConfig c = sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, configKey));
        String v = c == null ? "" : (c.getConfigValue() == null ? "" : c.getConfigValue());
        AdminResult r = AdminResult.success();
        r.put("msg", v);
        return r;
    }

    @PreAuthorize("@ss.hasPermi('system:config:add')")
    @PostMapping
    public AdminResult add(@RequestBody SysConfig config) {
        sysConfigMapper.insert(config);
        return AdminResult.success();
    }

    @PreAuthorize("@ss.hasPermi('system:config:edit')")
    @PutMapping
    public AdminResult edit(@RequestBody SysConfig config) {
        sysConfigMapper.updateById(config);
        return AdminResult.success();
    }

    @PreAuthorize("@ss.hasPermi('system:config:remove')")
    @DeleteMapping("/{configIds}")
    public AdminResult remove(@PathVariable String configIds) {
        for (String id : configIds.split(",")) {
            if (StringUtils.hasText(id)) {
                sysConfigMapper.deleteById(Integer.parseInt(id.trim()));
            }
        }
        return AdminResult.success();
    }

    @PreAuthorize("@ss.hasPermi('system:config:remove')")
    @DeleteMapping("/refreshCache")
    public AdminResult refreshCache() {
        return AdminResult.success();
    }
}
