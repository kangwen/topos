package com.topos.admin.framework.web.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.topos.admin.common.constant.CacheConstants;
import com.topos.admin.common.constant.Constants;
import com.topos.admin.common.core.domain.model.LoginUser;
import com.topos.admin.common.utils.IdUtils;
import com.topos.admin.common.utils.ServletUtils;
import com.topos.admin.common.utils.StringUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

/**
 *JWT 仅存 uuid + subject，完整 {@link LoginUser} 放 Redis。
 */
@Component
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    private static final long MILLIS_MINUTE_TWENTY = 20 * 60_000L;

    @Value("${token.header}")
    private String header;

    @Value("${token.secret}")
    private String secret;

    @Value("${token.expireTime}")
    private int expireTimeMinutes;

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private SecretKey secretKey;

    public TokenService(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void initSigningKey() {
        this.secretKey = hmacSha512Key(secret);
    }

    public LoginUser getLoginUser(HttpServletRequest request) {
        String token = getToken(request);
        if (StringUtils.isEmpty(token)) {
            return null;
        }
        try {
            Claims claims = parseToken(token);
            String uuid = claims.get(Constants.LOGIN_USER_KEY, String.class);
            if (StringUtils.isEmpty(uuid)) {
                return null;
            }
            String userKey = tokenCacheKey(uuid);
            String json = stringRedisTemplate.opsForValue().get(userKey);
            if (StringUtils.isEmpty(json)) {
                return null;
            }
            return objectMapper.readValue(json, LoginUser.class);
        } catch (Exception e) {
            log.error("获取用户信息异常'{}'", e.getMessage());
            return null;
        }
    }

    public void setLoginUser(LoginUser loginUser) {
        if (loginUser != null && StringUtils.isNotEmpty(loginUser.getToken())) {
            refreshToken(loginUser);
        }
    }

    public void delLoginUser(String uuid) {
        if (StringUtils.isNotEmpty(uuid)) {
            stringRedisTemplate.delete(tokenCacheKey(uuid));
        }
    }

    public String createToken(LoginUser loginUser) {
        String uuid = IdUtils.fastUUID();
        loginUser.setToken(uuid);
        setUserAgent(loginUser);
        refreshToken(loginUser);
        return Jwts.builder()
                .claim(Constants.LOGIN_USER_KEY, uuid)
                .subject(loginUser.getUsername())
                .signWith(secretKey)
                .compact();
    }

    public void verifyToken(LoginUser loginUser) {
        long expireAt = loginUser.getExpireTime();
        long now = System.currentTimeMillis();
        if (expireAt - now <= MILLIS_MINUTE_TWENTY) {
            refreshToken(loginUser);
        }
    }

    public void refreshToken(LoginUser loginUser) {
        long now = System.currentTimeMillis();
        loginUser.setLoginTime(now);
        loginUser.setExpireTime(now + (long) expireTimeMinutes * 60_000L);
        String userKey = tokenCacheKey(loginUser.getToken());
        try {
            String json = objectMapper.writeValueAsString(loginUser);
            stringRedisTemplate.opsForValue().set(userKey, json, expireTimeMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new IllegalStateException("缓存登录用户失败", e);
        }
    }

    private void setUserAgent(LoginUser loginUser) {
        try {
            HttpServletRequest request = ServletUtils.getRequest();
            loginUser.setIpaddr(request.getRemoteAddr());
        } catch (Exception ignored) {
            loginUser.setIpaddr("");
        }
        loginUser.setLoginLocation("");
        loginUser.setBrowser("");
        loginUser.setOs("");
    }

    private Claims parseToken(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String getToken(HttpServletRequest request) {
        String t = request.getHeader(header);
        if (StringUtils.isNotEmpty(t) && t.startsWith(Constants.TOKEN_PREFIX)) {
            return t.substring(Constants.TOKEN_PREFIX.length()).trim();
        }
        return StringUtils.trim(t);
    }

    private String tokenCacheKey(String uuid) {
        return CacheConstants.LOGIN_TOKEN_KEY + uuid;
    }

    private static SecretKey hmacSha512Key(String rawSecret) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] digest = md.digest(rawSecret.getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-512 not available", e);
        }
    }
}
