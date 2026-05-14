package com.topos.admin.framework.config;

import com.topos.admin.framework.security.filter.JwtAuthenticationTokenFilter;
import com.topos.admin.framework.security.handle.AccessDeniedHandlerImpl;
import com.topos.admin.framework.security.handle.AuthenticationEntryPointImpl;
import com.topos.admin.framework.security.handle.LogoutSuccessHandlerImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import com.topos.admin.framework.security.AdminAuthenticationProvider;

import java.util.Arrays;
import java.util.List;

/**
 * JWT + Redis + 无 Session。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class AdminSecurityConfig {

    private final AuthenticationEntryPointImpl unauthorizedHandler;
    private final LogoutSuccessHandlerImpl logoutSuccessHandler;
    private final JwtAuthenticationTokenFilter authenticationTokenFilter;
    private final AccessDeniedHandlerImpl accessDeniedHandler;

    public AdminSecurityConfig(
            AuthenticationEntryPointImpl unauthorizedHandler,
            LogoutSuccessHandlerImpl logoutSuccessHandler,
            JwtAuthenticationTokenFilter authenticationTokenFilter,
            AccessDeniedHandlerImpl accessDeniedHandler) {
        this.unauthorizedHandler = unauthorizedHandler;
        this.logoutSuccessHandler = logoutSuccessHandler;
        this.authenticationTokenFilter = authenticationTokenFilter;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public CorsFilter corsFilter(@Value("${topos.cors.allowed-origins:http://localhost:80,http://localhost:1024,http://127.0.0.1:1024,http://localhost:8090,http://127.0.0.1:80}") String origins) {
        CorsConfiguration config = new CorsConfiguration();
        List<String> list = Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        config.setAllowedOriginPatterns(list.isEmpty() ? List.of("*") : list);
        config.setAllowCredentials(true);
        config.addAllowedHeader(CorsConfiguration.ALL);
        config.addAllowedMethod(CorsConfiguration.ALL);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    @Bean
    public AuthenticationManager authenticationManager(AdminAuthenticationProvider adminAuthenticationProvider) {
        return new ProviderManager(List.of(adminAuthenticationProvider));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity, CorsFilter corsFilter) throws Exception {
        return httpSecurity
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers
                        .cacheControl(cache -> cache.disable())
                        .frameOptions(frame -> frame.sameOrigin()))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(unauthorizedHandler)
                        .accessDeniedHandler(accessDeniedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/login", "/register", "/captchaImage").permitAll()
                        // PathPattern 不允许 /** 后仍有片段（如 /**/*.html）；深层静态资源用 Ant 风格匹配
                        .requestMatchers(HttpMethod.GET, "/", "/*.html").permitAll()
                        .requestMatchers(
                                new AntPathRequestMatcher("/**/*.html", HttpMethod.GET.name()),
                                new AntPathRequestMatcher("/**/*.css", HttpMethod.GET.name()),
                                new AntPathRequestMatcher("/**/*.js", HttpMethod.GET.name()))
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/profile/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/v3/api-docs/**", "/swagger-ui/**", "/druid/**").permitAll()
                        .requestMatchers("/actuator/**", "/error").permitAll()
                        .anyRequest().authenticated())
                .logout(logout -> logout.logoutUrl("/logout").logoutSuccessHandler(logoutSuccessHandler))
                .addFilterBefore(authenticationTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(corsFilter, JwtAuthenticationTokenFilter.class)
                .addFilterBefore(corsFilter, LogoutFilter.class)
                .build();
    }
}
