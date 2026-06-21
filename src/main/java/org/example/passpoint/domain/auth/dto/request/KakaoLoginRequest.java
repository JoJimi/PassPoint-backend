package org.example.passpoint.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 카카오 로그인 요청 - 앱이 카카오 SDK로 받은 액세스 토큰을 담아 보냄
 */
public record KakaoLoginRequest(
        @NotBlank(message = "accessToken은 필수입니다.")
        String accessToken
) {
}
