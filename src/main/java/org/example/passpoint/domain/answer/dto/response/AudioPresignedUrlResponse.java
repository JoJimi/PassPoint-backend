package org.example.passpoint.domain.answer.dto.response;

public record AudioPresignedUrlResponse(
        String uploadUrl,
        String key
) {
}
