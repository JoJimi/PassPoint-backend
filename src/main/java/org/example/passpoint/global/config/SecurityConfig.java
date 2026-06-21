package org.example.passpoint.global.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.passpoint.global.jwt.JwtAuthenticationFilter;
import org.example.passpoint.global.jwt.JwtProvider;
import org.example.passpoint.global.jwt.filter.JwtAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final StringRedisTemplate redisTemplate;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    /** 이메일 회원가입/로그인 비밀번호 해싱용 */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // JWT 사용 → 폼로그인/httpBasic/csrf 비활성화
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // 세션 사용 안 함 (토큰으로 인증)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 경로별 권한
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/login/**",    // 로그인은 공개
                                "/api/v1/auth/signup/**",   // 회원가입도 공개
                                "/api/v1/auth/refresh",     // 갱신도 공개
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/actuator/**"
                        ).permitAll()
                        .anyRequest().authenticated()       // 나머지는 인증 필요
                )
                // 인증 안 된 요청 -> 401로 응답 (기본 403 대신)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                // JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 등록
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtProvider, redisTemplate),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
