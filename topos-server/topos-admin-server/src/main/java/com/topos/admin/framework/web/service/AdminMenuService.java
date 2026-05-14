package com.topos.admin.framework.web.service;

import com.topos.admin.common.constant.Constants;
import com.topos.admin.common.core.domain.entity.SysMenu;
import com.topos.admin.common.core.domain.entity.SysRole;
import com.topos.admin.common.core.domain.entity.SysUser;
import com.topos.admin.common.core.domain.model.LoginUser;
import com.topos.admin.common.utils.StringUtils;
import com.topos.admin.system.mapper.SysMenuMapper;
import com.topos.admin.web.domain.vo.MetaVo;
import com.topos.admin.web.domain.vo.RouterVo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 从 {@code sys_menu} 构建前端动态路由（行为对齐 ruoyi-vue {@code getRouters}）。
 */
@Service
public class AdminMenuService {

    private static final long MENU_ROOT_ID = 0L;
    private static final String TYPE_DIR = "M";
    private static final String TYPE_MENU = "C";
    private static final String LAYOUT = "Layout";
    private static final String PARENT_VIEW = "ParentView";
    private static final String INNER_LINK = "InnerLink";

    private static final String[] INNER_SEARCH = {"http://", "https://", "www.", ".", ":"};
    private static final String[] INNER_REPL = {"", "", "", "/", "/"};

    private final SysMenuMapper sysMenuMapper;

    public AdminMenuService(SysMenuMapper sysMenuMapper) {
        this.sysMenuMapper = sysMenuMapper;
    }

    public List<RouterVo> buildRouters(LoginUser loginUser) {
        if (loginUser == null || loginUser.getUser() == null) {
            return List.of();
        }
        SysUser u = loginUser.getUser();
        boolean allMenus = u.isAdmin()
                || (loginUser.getPermissions() != null
                && loginUser.getPermissions().contains(Constants.ALL_PERMISSION));
        if (!allMenus && u.getRoles() != null) {
            for (SysRole r : u.getRoles()) {
                if (r.isAdmin()) {
                    allMenus = true;
                    break;
                }
            }
        }
        List<SysMenu> flat = allMenus
                ? sysMenuMapper.selectMenuTreeAll()
                : expandWithAncestors(sysMenuMapper.selectMenusByUserId(u.getUserId()));
        List<SysMenu> tree = getChildPerms(flat, MENU_ROOT_ID);
        List<RouterVo> routers = new LinkedList<>();
        for (SysMenu m : tree) {
            RouterVo r = buildMenu(m, null);
            if (r != null) {
                routers.add(r);
            }
        }
        return routers;
    }

    private List<SysMenu> expandWithAncestors(List<SysMenu> menus) {
        if (menus == null || menus.isEmpty()) {
            return List.of();
        }
        List<SysMenu> all = new ArrayList<>(menus);
        Set<Long> have = new HashSet<>();
        for (SysMenu m : all) {
            have.add(m.getMenuId());
        }
        boolean more = true;
        while (more) {
            more = false;
            List<SysMenu> snapshot = new ArrayList<>(all);
            for (SysMenu m : snapshot) {
                Long pid = m.getParentId();
                if (pid == null || pid <= 0 || have.contains(pid)) {
                    continue;
                }
                SysMenu p = sysMenuMapper.selectById(pid);
                if (p != null && (TYPE_DIR.equals(p.getMenuType()) || TYPE_MENU.equals(p.getMenuType()))
                        && "0".equals(p.getVisible())) {
                    all.add(p);
                    have.add(p.getMenuId());
                    more = true;
                }
            }
        }
        all.sort(Comparator.comparing(SysMenu::getParentId, Comparator.nullsFirst(Long::compareTo))
                .thenComparing(SysMenu::getOrderNum, Comparator.nullsLast(Integer::compareTo)));
        return all;
    }

    private List<SysMenu> getChildPerms(List<SysMenu> list, long parentId) {
        List<SysMenu> returnList = new ArrayList<>();
        for (SysMenu t : list) {
            if (Objects.equals(t.getParentId(), parentId)) {
                recursionFn(list, t);
                returnList.add(t);
            }
        }
        return returnList;
    }

    private void recursionFn(List<SysMenu> list, SysMenu t) {
        List<SysMenu> childList = getChildList(list, t);
        t.setChildren(childList);
        for (SysMenu c : childList) {
            if (hasChild(list, c)) {
                recursionFn(list, c);
            }
        }
    }

    private List<SysMenu> getChildList(List<SysMenu> list, SysMenu parent) {
        List<SysMenu> out = new ArrayList<>();
        for (SysMenu n : list) {
            if (parent.getMenuId() != null && parent.getMenuId().equals(n.getParentId())) {
                out.add(n);
            }
        }
        out.sort(Comparator.comparing(SysMenu::getOrderNum, Comparator.nullsLast(Integer::compareTo)));
        return out;
    }

    private boolean hasChild(List<SysMenu> list, SysMenu t) {
        return !getChildList(list, t).isEmpty();
    }

    private RouterVo buildMenu(SysMenu menu, String layoutPrefix) {
        List<SysMenu> cMenus = menu.getChildren() != null ? menu.getChildren() : List.of();
        if (TYPE_DIR.equals(menu.getMenuType()) && cMenus.isEmpty()) {
            return null;
        }
        if (!cMenus.isEmpty() && TYPE_DIR.equals(menu.getMenuType())) {
            return buildDirectory(menu, layoutPrefix, cMenus);
        }
        if (isMenuFrame(menu)) {
            return buildMenuFrame(menu);
        }
        if (isRootInnerLink(menu)) {
            return buildRootInnerLink(menu);
        }
        return buildLeaf(menu, layoutPrefix);
    }

    private RouterVo buildDirectory(SysMenu menu, String layoutPrefix, List<SysMenu> cMenus) {
        RouterVo router = new RouterVo();
        router.setHidden("1".equals(menu.getVisible()));
        router.setName(routeName(menu));
        String path = getRouterPath(menu, layoutPrefix);
        router.setPath(path);
        router.setComponent(getComponent(menu));
        router.setQuery(null);
        router.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), false, null));
        router.setAlwaysShow(true);
        router.setRedirect("noRedirect");
        String next = nextLayoutPrefix(layoutPrefix, path, menu);
        List<RouterVo> kids = new LinkedList<>();
        for (SysMenu c : cMenus) {
            RouterVo r = buildMenu(c, next);
            if (r != null) {
                kids.add(r);
            }
        }
        router.setChildren(kids);
        return router;
    }

    private RouterVo buildMenuFrame(SysMenu menu) {
        RouterVo router = new RouterVo();
        router.setHidden("1".equals(menu.getVisible()));
        router.setName("");
        router.setPath("/");
        router.setComponent(LAYOUT);
        router.setQuery(null);
        router.setMeta(null);
        String url = menu.getUrl() == null ? "" : menu.getUrl();
        String rel = url.startsWith("/") ? url.substring(1) : url;
        RouterVo child = new RouterVo();
        child.setPath(rel);
        child.setComponent(urlToComponent(url));
        child.setName(routeName(menu));
        child.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), false, null));
        router.setChildren(List.of(child));
        return router;
    }

    private RouterVo buildRootInnerLink(SysMenu menu) {
        RouterVo router = new RouterVo();
        router.setHidden("1".equals(menu.getVisible()));
        router.setName(routeName(menu));
        router.setPath("/");
        router.setComponent(LAYOUT);
        router.setQuery(null);
        router.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon()));
        String url = menu.getUrl();
        RouterVo inner = new RouterVo();
        inner.setPath(innerLinkReplace(url));
        inner.setComponent(INNER_LINK);
        inner.setName(routeName(menu));
        inner.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), false, url));
        router.setChildren(List.of(inner));
        return router;
    }

    private RouterVo buildLeaf(SysMenu menu, String layoutPrefix) {
        RouterVo router = new RouterVo();
        router.setHidden("1".equals(menu.getVisible()));
        router.setName(routeName(menu));
        router.setPath(getRouterPath(menu, layoutPrefix));
        router.setComponent(getComponent(menu));
        router.setQuery(null);
        String link = isHttp(menu.getUrl()) ? menu.getUrl() : null;
        router.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), false, link));
        return router;
    }

    private String nextLayoutPrefix(String parentPrefix, String routerPath, SysMenu menu) {
        if (!TYPE_DIR.equals(menu.getMenuType())) {
            return parentPrefix;
        }
        if (parentPrefix == null || parentPrefix.isEmpty()) {
            return routerPath.startsWith("/") ? routerPath : "/" + routerPath;
        }
        if (routerPath.startsWith("/")) {
            return routerPath;
        }
        return parentPrefix + "/" + routerPath;
    }

    private String getRouterPath(SysMenu menu, String layoutPrefix) {
        long pid = menu.getParentId() == null ? 0L : menu.getParentId();
        String url = menu.getUrl() == null ? "" : menu.getUrl();

        if (pid != MENU_ROOT_ID && isHttp(url)) {
            return innerLinkReplace(url);
        }
        if (pid == MENU_ROOT_ID && TYPE_DIR.equals(menu.getMenuType()) && !isHttp(url)) {
            if ("#".equals(url) || url.isEmpty()) {
                return inferRootPath(menu);
            }
            if (!url.startsWith("/")) {
                return "/" + url;
            }
            return url;
        }
        if (isMenuFrame(menu)) {
            return "/";
        }
        if (isHttp(url)) {
            return innerLinkReplace(url);
        }
        if (url.startsWith("/") && layoutPrefix != null && url.startsWith(layoutPrefix + "/")) {
            return url.substring(layoutPrefix.length() + 1);
        }
        if (url.startsWith("/")) {
            return url.substring(1);
        }
        return url;
    }

    private String inferRootPath(SysMenu menu) {
        String u = firstLeafUrl(menu);
        if (u != null && u.startsWith("/")) {
            int end = u.indexOf('/', 1);
            return end < 0 ? u : u.substring(0, end);
        }
        return "/m" + menu.getMenuId();
    }

    private String firstLeafUrl(SysMenu menu) {
        if (menu.getChildren() == null) {
            return null;
        }
        for (SysMenu c : menu.getChildren()) {
            String u = firstLeafUrlDeep(c);
            if (u != null) {
                return u;
            }
        }
        return null;
    }

    private String firstLeafUrlDeep(SysMenu m) {
        if (TYPE_MENU.equals(m.getMenuType())) {
            String u = m.getUrl();
            if (StringUtils.isNotEmpty(u) && !"#".equals(u) && !isHttp(u)) {
                return u;
            }
        }
        if (m.getChildren() != null) {
            for (SysMenu c : m.getChildren()) {
                String u = firstLeafUrlDeep(c);
                if (u != null) {
                    return u;
                }
            }
        }
        return null;
    }

    private boolean isMenuFrame(SysMenu menu) {
        long pid = menu.getParentId() == null ? 0L : menu.getParentId();
        String url = menu.getUrl();
        return pid == MENU_ROOT_ID && TYPE_MENU.equals(menu.getMenuType())
                && !isHttp(url) && !"#".equals(url);
    }

    private boolean isRootInnerLink(SysMenu menu) {
        long pid = menu.getParentId() == null ? 0L : menu.getParentId();
        return pid == MENU_ROOT_ID && isHttp(menu.getUrl());
    }

    private static boolean isHttp(String url) {
        return url != null && (url.startsWith("http://") || url.startsWith("https://"));
    }

    private static String innerLinkReplace(String path) {
        if (path == null) {
            return "";
        }
        return StringUtils.replaceEach(path, INNER_SEARCH, INNER_REPL);
    }

    private String getComponent(SysMenu menu) {
        if (TYPE_DIR.equals(menu.getMenuType())) {
            long pid = menu.getParentId() == null ? 0L : menu.getParentId();
            if (pid != MENU_ROOT_ID) {
                return PARENT_VIEW;
            }
            return LAYOUT;
        }
        if (TYPE_MENU.equals(menu.getMenuType())) {
            if (isHttp(menu.getUrl())) {
                return INNER_LINK;
            }
            return urlToComponent(menu.getUrl());
        }
        return LAYOUT;
    }

    private static String urlToComponent(String url) {
        if (url == null || isHttp(url)) {
            return INNER_LINK;
        }
        String p = url.startsWith("/") ? url.substring(1) : url;
        if (p.endsWith("/index")) {
            return p;
        }
        return p + "/index";
    }

    private String routeName(SysMenu menu) {
        if (isMenuFrame(menu)) {
            return "";
        }
        String url = menu.getUrl();
        if (StringUtils.isNotEmpty(url) && !"#".equals(url) && !isHttp(url)) {
            String p = url.replaceFirst("^/", "").replace('/', '_');
            if (!p.isEmpty()) {
                return StringUtils.capitalize(p);
            }
        }
        if (isHttp(url)) {
            return "M" + menu.getMenuId();
        }
        return "M" + menu.getMenuId();
    }
}
