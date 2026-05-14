package com.topos.admin.common.core.domain;

import com.topos.admin.common.constant.HttpStatus;
import com.topos.admin.common.utils.StringUtils;

import java.util.HashMap;
import java.util.Objects;

/**
 * 管理端统一响应体
 */
public class AdminResult extends HashMap<String, Object> {

    private static final long serialVersionUID = 1L;

    public static final String CODE_TAG = "code";
    public static final String MSG_TAG = "msg";
    public static final String DATA_TAG = "data";

    public AdminResult() {
    }

    public AdminResult(int code, String msg) {
        super.put(CODE_TAG, code);
        super.put(MSG_TAG, msg);
    }

    public AdminResult(int code, String msg, Object data) {
        super.put(CODE_TAG, code);
        super.put(MSG_TAG, msg);
        if (StringUtils.isNotNull(data)) {
            super.put(DATA_TAG, data);
        }
    }

    public static AdminResult success() {
        return AdminResult.success("操作成功");
    }

    public static AdminResult success(Object data) {
        return AdminResult.success("操作成功", data);
    }

    public static AdminResult success(String msg) {
        return AdminResult.success(msg, null);
    }

    public static AdminResult success(String msg, Object data) {
        return new AdminResult(HttpStatus.SUCCESS, msg, data);
    }

    public static AdminResult warn(String msg) {
        return AdminResult.warn(msg, null);
    }

    public static AdminResult warn(String msg, Object data) {
        return new AdminResult(HttpStatus.WARN, msg, data);
    }

    public static AdminResult error() {
        return AdminResult.error("操作失败");
    }

    public static AdminResult error(String msg) {
        return AdminResult.error(msg, null);
    }

    public static AdminResult error(String msg, Object data) {
        return new AdminResult(HttpStatus.ERROR, msg, data);
    }

    public static AdminResult error(int code, String msg) {
        return new AdminResult(code, msg, null);
    }

    public boolean isSuccess() {
        return Objects.equals(HttpStatus.SUCCESS, this.get(CODE_TAG));
    }

    public boolean isWarn() {
        return Objects.equals(HttpStatus.WARN, this.get(CODE_TAG));
    }

    public boolean isError() {
        return Objects.equals(HttpStatus.ERROR, this.get(CODE_TAG));
    }

    @Override
    public AdminResult put(String key, Object value) {
        super.put(key, value);
        return this;
    }
}
