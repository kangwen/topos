package com.topos.admin.config;

import com.topos.admin.common.constant.HttpStatus;
import com.topos.admin.common.core.domain.AdminResult;
import com.topos.admin.common.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常：统一返回 AdminResult
 */
@RestControllerAdvice
public class AdminGlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AdminGlobalExceptionHandler.class);

    @ExceptionHandler(ServiceException.class)
    public AdminResult handleServiceException(ServiceException e) {
        Integer code = e.getCode();
        return AdminResult.error(code != null ? code : HttpStatus.ERROR, e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public AdminResult handleValidationException(Exception e) {
        return AdminResult.error(HttpStatus.BAD_REQUEST, "请求参数不合法");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public AdminResult handleBadCredentials(BadCredentialsException e) {
        return AdminResult.error(HttpStatus.UNAUTHORIZED, "用户或密码错误");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public AdminResult handleIllegalArgument(IllegalArgumentException e) {
        return AdminResult.error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public AdminResult handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("Data integrity: {}", e.getMessage());
        return AdminResult.error(HttpStatus.BAD_REQUEST, "数据仍被引用或违反约束，无法完成操作");
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public AdminResult handleUnsupported(UnsupportedOperationException e) {
        return AdminResult.error(HttpStatus.ERROR, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public AdminResult handleException(Exception e) {
        log.error(e.getMessage(), e);
        return AdminResult.error(HttpStatus.ERROR, "服务器内部错误");
    }
}
