package com.topos.admin.common.utils;

import org.springframework.util.AntPathMatcher;

import java.util.Collection;
import java.util.Map;

/**
 * 轻量字符串/空值工具
 */
public final class StringUtils {

    public static final String EMPTY = "";

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private StringUtils() {
    }

    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    public static boolean hasText(String str) {
        return org.springframework.util.StringUtils.hasText(str);
    }

    public static boolean isNull(Object obj) {
        return obj == null;
    }

    public static boolean isNotNull(Object obj) {
        return obj != null;
    }

    public static String trim(String str) {
        return str == null ? null : str.trim();
    }

    public static boolean inStringIgnoreCase(String str, String... searchStrs) {
        if (str == null || searchStrs == null) {
            return false;
        }
        for (String s : searchStrs) {
            if (s != null && str.equalsIgnoreCase(s)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isEmpty(Collection<?> coll) {
        return coll == null || coll.isEmpty();
    }

    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    public static boolean simpleMatch(String pattern, String str) {
        if (pattern == null || str == null) {
            return false;
        }
        return MATCHER.match(pattern, str);
    }

    /**
     * 顺序替换子串（用于内链路由 path 与 RuoYi 行为对齐）。
     */
    public static String replaceEach(String text, String[] searchList, String[] replacementList) {
        if (text == null || searchList == null || replacementList == null
                || searchList.length != replacementList.length) {
            return text;
        }
        String result = text;
        for (int i = 0; i < searchList.length; i++) {
            String s = searchList[i];
            String r = replacementList[i];
            if (s != null) {
                result = result.replace(s, r == null ? "" : r);
            }
        }
        return result;
    }

    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
