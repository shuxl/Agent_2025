package com.lance.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security Configuration - 允许API端点公开访问
 * 
 * @author lance
 */
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                // 允许所有API端点公开访问
                .requestMatchers("/api/**").permitAll()
                // 允许健康检查端点
                .requestMatchers("/actuator/**").permitAll()
                // 允许MCP相关端点
                .requestMatchers("/mcp/**").permitAll()
                // 允许SSE端点（如果需要认证，可以单独配置）
                .requestMatchers("/sse").permitAll()
                // 其他请求需要认证
                .anyRequest().authenticated()
            )
            .csrf(CsrfConfigurer::disable)
            .cors(cors -> cors.disable())
            .build();
    }
}
