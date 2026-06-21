package org.example.passpoint.domain.auth.dto.response;

/**
 * 카카오 사용자 정보 조회(/v2/user/me) 응답에서 추출한 사용자 정보
 * - oauthId: 카카오 고유 회원번호(id)
 * - email: 이메일 동의 항목을 거부했거나 비즈 앱 미전환 상태면 null일 수 있음 (KakaoOAuthClient가 폴백 처리)
 */
public record KakaoUserInfo(
        String oauthId,
        String email,
        String nickname
) {
}
