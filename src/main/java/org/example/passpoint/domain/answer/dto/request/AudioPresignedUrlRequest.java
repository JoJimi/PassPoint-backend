package org.example.passpoint.domain.answer.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AudioPresignedUrlRequest(
        @NotBlank String fileExtension
) {
}
