package com.topos.admin.web.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 动态路由节点
 */
@Data
public class RouterVo {

    private String name;
    private String path;
    private boolean hidden;
    private String redirect;
    private String component;
    private String query;
    private Boolean alwaysShow;
    private MetaVo meta;
    private List<RouterVo> children;
}
