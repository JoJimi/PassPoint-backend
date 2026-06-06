package org.example.passpoint.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // REST API라 CSRF 보호는 필요 없으므로 비활성화
                .csrf(AbstractHttpConfigurer::disable)
                // 기본 폼 로그인 / HTTP Basic 끄기 (랜덤 비번 로그인 창 제거)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // 경로별 권한
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/actuator/**"
                        ).permitAll() // Swagger 관련 경로 인증 없이 허용
                        // 1주차엔 아직 인증 로직이 없으니 일단 전부 허용
                        // (인증 이식 단계에서 .authenticated()로 조여나감)
                        .anyRequest().permitAll()
            );

        return http.build();
    }
}
