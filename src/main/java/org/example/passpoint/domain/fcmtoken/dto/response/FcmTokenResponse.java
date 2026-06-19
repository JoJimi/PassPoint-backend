package org.example.passpoint.domain.fcmtoken.dto.response;

import org.example.passpoint.domain.fcmtoken.entity.FcmToken;

import java.time.LocalDateTime;

/**
 * FCM 토큰 등록 응답
 */
public record FcmTokenResponse(
        Long tokenId,
        String token,
        LocalDateTime createdAt
) {
    public static FcmTokenResponse from(FcmToken fcmToken) {
        return new FcmTokenResponse(
                fcmToken.getId(),
                fcmToken.getToken(),
                fcmToken.getCreatedAt()
        );
    }
}
