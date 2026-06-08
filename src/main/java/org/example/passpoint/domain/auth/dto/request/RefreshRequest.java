package org.example.passpoint.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 토큰 갱신 요청 - 클라이언트가 보관 중인 refreshToken을 보냄
 */
public record RefreshRequest(
        @NotBlank(message = "refreshToken은 필수입니다.")
        String refreshToken
) {
}
