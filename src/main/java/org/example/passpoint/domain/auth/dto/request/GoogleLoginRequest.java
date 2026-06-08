package org.example.passpoint.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 구글 로그인 요청 - 앱이 구글에서 받은 ID 토큰을 담아 보냄
 */
public record GoogleLoginRequest(
        @NotBlank(message = "idToken은 필수입니다.")
        String idToken
) {
}
