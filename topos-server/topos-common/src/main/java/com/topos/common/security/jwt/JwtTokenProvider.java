package com.topos.common.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Date;
import java.util.Map;

/**
 * JJWT 封装：签发与验签（各应用通过配置注入不同密钥与有效期）。
 */
public final class JwtTokenProvider {

    private final SecretKey key;
    private final Duration validity;

    public JwtTokenProvider(String secret, Duration validity) {
        this.key = hmacSha256Key(secret);
        this.validity = validity;
    }

    private static SecretKey hmacSha256Key(String secret) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(secret.getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public String createToken(String subject, Map<String, Object> extraClaims) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + validity.toMillis());
        var builder = Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .expiration(exp);
        if (extraClaims != null) {
            for (var e : extraClaims.entrySet()) {
                builder.claim(e.getKey(), e.getValue());
            }
        }
        return builder.signWith(key).compact();
    }

    public Jws<Claims> parse(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
    }

    public boolean validate(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException ignored) {
            return false;
        }
    }
}
