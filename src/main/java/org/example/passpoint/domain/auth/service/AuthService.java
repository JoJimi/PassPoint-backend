package org.example.passpoint.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.example.passpoint.domain.auth.client.GoogleOAuthClient;
import org.example.passpoint.domain.auth.dto.response.GoogleUserInfo;
import org.example.passpoint.domain.auth.dto.response.TokenResponse;
import org.example.passpoint.domain.user.entity.OAuthProvider;
import org.example.passpoint.domain.user.entity.User;
import org.example.passpoint.domain.user.repository.UserRepository;
import org.example.passpoint.global.exception.auth.RefreshTokenMisMatchException;
import org.example.passpoint.global.jwt.JwtProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * 인증 관련 핵심 비즈니스 로직
 * - 로그인: 구글 ID 토큰 검증 → 로그인/가입 분기 → JWT 발급 → Refresh를 Redis에 저장
 * - 토큰 갱신: Refresh 검증 + 재사용 탐지 → 새 토큰 발급(Rotation)
 * - 로그아웃: Refresh 삭제 + Access를 블랙리스트 등록
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final GoogleOAuthClient googleOAuthClient;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final StringRedisTemplate redisTemplate;

    private static final String REFRESH_KEY_PREFIX = "refresh:";
    private static final String BLACKLIST_KEY_PREFIX = "blacklist:";
    private static final Duration REFRESH_TTL = Duration.ofDays(14);

    /** 구글 로그인 - ID 토큰을 받아 사용자를 인증하고 JWT를 발급 */
    @Transactional
    public TokenResponse loginWithGoogle(String idToken) {
        // 1. 구글 ID 토큰 검증 -> 사용자 정보 추출
        GoogleUserInfo googleUser = googleOAuthClient.verify(idToken);

        // 2. 기존 회원이면 조회, 없으면 신규 가입
        User user = userRepository
                .findByOauthProviderAndOauthId(OAuthProvider.GOOGLE, googleUser.oauthId())
                .orElseGet(() -> registerNewUser(googleUser));

        // 3. 토큰 발급 + Refresh 저장
        return issueTokens(user.getId());
    }

    /** 토큰 갱신 - refreshToken 검증 후 새 토큰 쌍 발급 (롤링 + 재사용 탐지) */
    @Transactional(readOnly = true)
    public TokenResponse reissue(String refreshToken) {
        // 1. refreshToken 자체 검증 (위조·만료) - 실패 시 JwtProvider가 예외
        jwtProvider.validateToken(refreshToken);

        // 2. 토큰에서 userId 추출
        Long userId = jwtProvider.getUserId(refreshToken);

        // 3. Redis에 저장된 refreshToken과 비교
        String storedToken = redisTemplate.opsForValue().get(REFRESH_KEY_PREFIX + userId);

        if(storedToken == null || !storedToken.equals(refreshToken)) {
            // 저장된 게 없거나(만료/로그아웃) 다르면(재사용 의심)
            // → 안전하게 해당 유저의 refresh를 삭제 (탈취 대응: 강제 로그아웃)
            redisTemplate.delete(REFRESH_KEY_PREFIX + userId);
            throw new RefreshTokenMisMatchException();
        }

        // 4. 정상 → 새 토큰 쌍 발급 + Redis 갱신(롤링)
        return issueTokens(userId);
    }

    /** 로그아웃 - refresh 삭제 + access 토큰 블랙리스트 등록 */
    public void logout(Long userId, String accessToken) {
        // 1. Redis의 refreshToken 삭제 → 더 이상 갱신 불가
        redisTemplate.delete(REFRESH_KEY_PREFIX + userId);

        // 2. accessToken을 블랙리스트에 등록 (남은 유효시간만큼만 TTL)
        long remaining = jwtProvider.getRemainingValidity(accessToken);
        if(remaining > 0) {
            redisTemplate.opsForValue().set(
                    BLACKLIST_KEY_PREFIX + accessToken,
                    "logout",
                    Duration.ofMillis(remaining)
            );
        }
    }

    /** 구글 신규 사용자 가입 처리 */
    private User registerNewUser(GoogleUserInfo googleUser) {
        User newUser = User.builder()
                .oauthProvider(OAuthProvider.GOOGLE)
                .oauthId(googleUser.oauthId())
                .email(googleUser.email())
                .nickname(googleUser.name())        // 초기 닉네임은 구글 이름으로
                .build();

        return userRepository.save(newUser);
    }

    /** Access/Refresh 토큰 발급 후 Refresh를 Redis에 저장 */
    private TokenResponse issueTokens(Long userId) {
        String accessToken = jwtProvider.createAccessToken(userId);
        String refreshToken = jwtProvider.createRefreshToken(userId);

        // Redis에 "refresh: {userId}" -> refreshToken 저장 (TTL 14일)
        // 같은 userId로 다시 로그인하면 덮어쓰기 = 자연스러운 롤링
        redisTemplate.opsForValue().set(
                REFRESH_KEY_PREFIX + userId,
                refreshToken,
                REFRESH_TTL
        );

        return new TokenResponse(accessToken, refreshToken);
    }
}
