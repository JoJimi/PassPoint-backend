package org.example.passpoint.domain.auth.dto.response;

/**
 * 구글 ID 토큰 검증 후 추출한 사용자 정보
 * - oauthId: 구글이 발급한 고유 식별자(sub)
 * - email, name: 프로필 정보
 */
public record GoogleUserInfo(
        String oauthId,
        String email,
        String name
) {
}
