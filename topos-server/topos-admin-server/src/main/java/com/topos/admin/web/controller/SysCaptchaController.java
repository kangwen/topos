package com.topos.admin.web.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.topos.admin.common.core.domain.AdminResult;
import com.topos.admin.framework.web.service.AdminCaptchaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.FastByteArrayOutputStream;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

/**
 * 图形验证码（Hutool LineCaptcha，JSON + Base64 + uuid）。
 */
@RestController
public class SysCaptchaController {

    private static final int CAPTCHA_WIDTH = 160;
    private static final int CAPTCHA_HEIGHT = 60;
    private static final int CODE_LEN = 4;
    /** 干扰线数量 */
    private static final int LINE_COUNT = 50;

    private final AdminCaptchaService adminCaptchaService;

    @Value("${topos.captcha.enabled:false}")
    private boolean captchaEnabled;

    public SysCaptchaController(AdminCaptchaService adminCaptchaService) {
        this.adminCaptchaService = adminCaptchaService;
    }

    @GetMapping("/captchaImage")
    public AdminResult getCode() {
        AdminResult ajax = AdminResult.success();
        ajax.put("captchaEnabled", captchaEnabled);
        if (!captchaEnabled) {
            return ajax;
        }
        String uuid = UUID.randomUUID().toString().replace("-", "");
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(CAPTCHA_WIDTH, CAPTCHA_HEIGHT, CODE_LEN, LINE_COUNT);
        String code = lineCaptcha.getCode();
        BufferedImage image = lineCaptcha.getImage();
        adminCaptchaService.save(uuid, code);
        FastByteArrayOutputStream os = new FastByteArrayOutputStream();
        try {
            // JPEG 在部分 JDK/精简镜像下无 ImageWriter，write 返回 false 且不落盘，导致 img 为空；PNG 在标准 JDK 中普遍可用
            boolean written = ImageIO.write(image, "png", os);
            if (!written || os.size() == 0) {
                return AdminResult.error("验证码生成失败");
            }
        } catch (IOException e) {
            return AdminResult.error("验证码生成失败");
        }
        ajax.put("uuid", uuid);
        ajax.put("img", Base64.getEncoder().encodeToString(os.toByteArray()));
        return ajax;
    }
}
