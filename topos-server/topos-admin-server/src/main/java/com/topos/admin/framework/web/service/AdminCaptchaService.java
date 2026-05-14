package com.topos.admin.framework.web.service;

import com.topos.admin.common.constant.Constants;
import com.topos.admin.common.exception.ServiceException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * 图形验证码存 Redis
 */
@Service
public class AdminCaptchaService {

    private final StringRedisTemplate stringRedisTemplate;

    public AdminCaptchaService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void save(String uuid, String code) {
        if (!StringUtils.hasText(uuid) || !StringUtils.hasText(code)) {
            return;
        }
        String key = Constants.CAPTCHA_CODE_KEY + uuid;
        stringRedisTemplate
                .opsForValue()
                .set(key, code.trim(), Constants.CAPTCHA_EXPIRATION_MINUTES, TimeUnit.MINUTES);
    }

    public void validateAndConsume(String uuid, String inputCode) {
        if (!StringUtils.hasText(uuid) || !StringUtils.hasText(inputCode)) {
            throw new ServiceException("验证码不能为空");
        }
        String key = Constants.CAPTCHA_CODE_KEY + uuid;
        String cached = stringRedisTemplate.opsForValue().get(key);
        stringRedisTemplate.delete(key);
        if (cached == null) {
            throw new ServiceException("验证码已失效");
        }
        if (!cached.equalsIgnoreCase(inputCode.trim())) {
            throw new ServiceException("验证码错误");
        }
    }
}
