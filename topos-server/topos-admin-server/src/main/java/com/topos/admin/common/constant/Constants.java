package com.topos.admin.common.constant;

import io.jsonwebtoken.Claims;

import java.util.Locale;

/**
 */
public final class Constants {

    public static final String UTF8 = "UTF-8";
    public static final Locale DEFAULT_LOCALE = Locale.SIMPLIFIED_CHINESE;

    public static final String SUCCESS = "0";
    public static final String FAIL = "1";

    public static final String ALL_PERMISSION = "*:*:*";
    public static final String SUPER_ADMIN = "admin";

    public static final String ROLE_DELIMITER = ",";
    public static final String PERMISSION_DELIMITER = ",";

    public static final String TOKEN = "token";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String LOGIN_USER_KEY = "login_user_key";
    public static final String JWT_USERNAME = Claims.SUBJECT;

    /** Redis 中图形验证码前缀 */
    public static final String CAPTCHA_CODE_KEY = "topos:admin:captcha:";

    public static final int CAPTCHA_EXPIRATION_MINUTES = 2;

    private Constants() {
    }
}
