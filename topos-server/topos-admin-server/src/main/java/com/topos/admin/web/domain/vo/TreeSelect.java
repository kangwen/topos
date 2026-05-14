package com.topos.admin.web.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 部门/菜单下拉树
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TreeSelect {

    private Long id;
    private String label;
    private boolean disabled;
    private List<TreeSelect> children = new ArrayList<>();

    public TreeSelect(Long id, String label, boolean disabled) {
        this.id = id;
        this.label = label;
        this.disabled = disabled;
    }
}
