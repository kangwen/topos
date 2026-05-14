package com.topos.common.api;

import java.io.Serializable;

/**
 * 统一 API 响应体（MVC 层返回）。
 */
public final class Rsp<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private int code;
    private String message;
    private T data;

    public Rsp() {
    }

    public Rsp(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Rsp<T> ok(T data) {
        return new Rsp<>(0, "ok", data);
    }

    public static <T> Rsp<T> ok() {
        return ok(null);
    }

    public static <T> Rsp<T> fail(int code, String message) {
        return new Rsp<>(code, message, null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
