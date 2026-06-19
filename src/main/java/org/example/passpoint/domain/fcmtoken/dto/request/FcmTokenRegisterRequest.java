package org.example.passpoint.domain.fcmtoken.dto.request;

import jakarta.validation.constraints.NotBlank;

public record FcmTokenRegisterRequest(
        @NotBlank String token
) {
}
