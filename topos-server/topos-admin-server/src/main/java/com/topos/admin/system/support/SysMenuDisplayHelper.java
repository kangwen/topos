package com.topos.admin.system.support;

import com.topos.admin.common.core.domain.entity.SysMenu;

/**
 * 菜单表无 component/status 等列时，为前端管理页补全展示字段。
 */
public final class SysMenuDisplayHelper {

    private SysMenuDisplayHelper() {
    }

    public static void enrich(SysMenu m) {
        if (m == null) {
            return;
        }
        String url = m.getUrl();
        if ("C".equals(m.getMenuType()) && url != null && url.startsWith("/") && !url.startsWith("http")) {
            m.setComponent(url.substring(1) + "/index");
        } else if (m.getComponent() == null) {
            m.setComponent("");
        }
        if (m.getStatus() == null) {
            m.setStatus("0");
        }
        if (m.getIsFrame() == null) {
            m.setIsFrame("1");
        }
        if (m.getIsCache() == null) {
            m.setIsCache("0");
        }
    }

    public static void enrichTree(java.util.List<SysMenu> menus) {
        if (menus == null) {
            return;
        }
        for (SysMenu m : menus) {
            enrich(m);
            enrichTree(m.getChildren());
        }
    }
}
