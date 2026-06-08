package org.example.passpoint.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.example.passpoint.domain.auth.client.GoogleOAuthClient;
import org.example.passpoint.domain.auth.dto.response.GoogleUserInfo;
import org.example.passpoint.domain.auth.dto.response.TokenResponse;
import org.example.passpoint.domain.user.entity.OAuthProvider;
import org.example.passpoint.domain.user.entity.User;
import org.example.passpoint.domain.user.repository.UserRepository;
import org.example.passpoint.global.jwt.JwtProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final GoogleOAuthClient googleOAuthClient;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final StringRedisTemplate redisTemplate;

    private static final String REFRESH_KEY_PREFIX = "refresh:";
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
