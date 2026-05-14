package com.topos.common.exception;

import lombok.Getter;

/**
 * 业务异常，由全局异常处理器转换为 HTTP 响应。
 */
@Getter
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public BizException(String message) {
        this(400, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

}
