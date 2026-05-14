package com.topos.strategy.config;

import com.topos.common.api.Rsp;
import com.topos.common.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = {"com.topos.strategy.controller"})
public class ApiGlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiGlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Rsp<Void>> handleBusiness(BizException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Rsp.fail(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<Rsp<Void>> handleValidation(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Rsp.fail(400, "请求参数不合法"));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Rsp<Void>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Rsp.fail(401, "用户名或密码错误"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Rsp<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Rsp.fail(400, ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Rsp<Void>> handleIllegalState(IllegalStateException ex) {
        String msg = ex.getMessage() == null ? "状态异常" : ex.getMessage();
        if (msg.contains("未登录")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Rsp.fail(401, msg));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Rsp.fail(400, msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Rsp<Void>> handleOthers(Exception ex) {
        log.error("Unhandled error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Rsp.fail(500, "服务器内部错误"));
    }
}
