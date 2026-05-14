package com.topos.web.config;

import com.topos.web.security.AnonymousPathFilter;
import com.topos.web.security.JwtAuthenticationFilter;
import com.topos.common.security.jwt.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * API：独立 JWT 密钥与过滤器链；免登录路径由 yml 列表 + {@link AnonymousPathFilter} 共同生效。
 */
@Configuration
@EnableWebSecurity
public class ApiSecurityConfig {

    /**
     * 过滤器只在 SecurityFilterChain 内注册；不要做成独立 {@code @Bean Filter}，否则 Boot 会再注册到 Servlet 容器，
     * 与链内实例叠加后 {@link org.springframework.web.filter.OncePerRequestFilter} 只执行一次，导致 JWT 被跳过、上下文被清空。
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiChain(
            HttpSecurity http,
            ApiSecurityProperties apiSecurityProperties,
            @Qualifier("jwtTokenProvider") JwtTokenProvider jwtTokenProvider)
            throws Exception {
        var anonymousPathFilter = new AnonymousPathFilter(apiSecurityProperties);
        var parentJwtAuthenticationFilter = new JwtAuthenticationFilter(jwtTokenProvider);
        http
                .securityMatcher("/api/v1/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    for (String pattern : apiSecurityProperties.getAnonymousPaths()) {
                        if (pattern != null && !pattern.isBlank()) {
                            auth.requestMatchers(pattern.trim()).permitAll();
                        }
                    }
                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(anonymousPathFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(parentJwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
