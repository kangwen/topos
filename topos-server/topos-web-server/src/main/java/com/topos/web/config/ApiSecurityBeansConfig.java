package com.topos.web.config;

import com.topos.common.security.jwt.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;

@Configuration
public class ApiSecurityBeansConfig {

    @Bean
    public PasswordEncoder apiPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager apiAuthenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public JwtTokenProvider jwtTokenProvider(
            @Value("${topos.security.web.jwt.secret}") String secret,
            @Value("${topos.security.web.jwt.expiration-minutes:10080}") long expirationMinutes) {
        return new JwtTokenProvider(secret, Duration.ofMinutes(expirationMinutes));
    }
    
}
