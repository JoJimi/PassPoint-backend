package org.example.passpoint.global.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * 모든 요청을 가로채 JWT를 검증하고 인증 정보를 설정하는 필터
 * - Authorization 헤더의 Bearer 토큰을 추출·검증
 * - 블랙리스트(로그아웃된 토큰) 확인 후, 유효하면 SecurityContext에 인증 정보 등록
 * - principal에 userId, credentials에 토큰 문자열을 담음 (로그아웃 시 토큰 재사용)
 * - 토큰이 없거나 무효면 그냥 통과(인증 안 된 상태로) → 보호된 API는 뒤에서 차단됨
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final StringRedisTemplate redisTemplate;

    private static final String BLACKLIST_KEY_PREFIX = "blacklist:";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        // 토큰이 있으면 검증 후 인증 정보 등록
        if(StringUtils.hasText(token) && jwtProvider.validateToken(token)) {
            // 블랙리스트 체크 - 로그아웃된 토큰이면 인증 안 함
            if(Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + token))) {
                filterChain.doFilter(request, response);
                return;         // 인증 정보 등록 안하고 통과 -> 뒤에서 401
            }

            Long userId = jwtProvider.getUserId(token);

            // 인증 객체 생성 - principal에 userId를 담음
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,         // principal
                            token,          // <- credential에 토큰 문자열 저장
                            Collections.emptyList()
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    /** Authorization 헤더에서 "Bearer " 떼고 순수 토큰만 추출 */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if(StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
