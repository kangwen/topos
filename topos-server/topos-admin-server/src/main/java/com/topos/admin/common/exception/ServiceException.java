package com.topos.admin.common.exception;

/**
 * 业务异常
 */
public class ServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Integer code;

    public ServiceException(String message) {
        this(message, 500);
    }

    public ServiceException(String message, Integer code) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
