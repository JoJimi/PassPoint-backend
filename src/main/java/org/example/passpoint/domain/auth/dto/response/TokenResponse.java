package org.example.passpoint.domain.auth.dto.response;

/**
 * 로그인/토큰갱신 시 클라이언트에게 내려주는 토큰 한 쌍
 */
public record TokenResponse(
        String accessToken,
        String refreshToken
) {
}
