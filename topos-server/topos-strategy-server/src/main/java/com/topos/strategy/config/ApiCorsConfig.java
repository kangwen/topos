package com.topos.strategy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class ApiCorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${topos.cors.api.allowed-origins:http://localhost:5173,http://localhost:5174,http://127.0.0.1:5174,http://localhost:8080}") String origins) {
        CorsConfiguration config = new CorsConfiguration();
        List<String> originList =
                Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        config.setAllowedOrigins(originList);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/strategy/**", config);
        return source;
    }
}
