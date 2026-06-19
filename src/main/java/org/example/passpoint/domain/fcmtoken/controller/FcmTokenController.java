package org.example.passpoint.domain.fcmtoken.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.passpoint.domain.fcmtoken.dto.request.FcmTokenRegisterRequest;
import org.example.passpoint.domain.fcmtoken.dto.response.FcmTokenResponse;
import org.example.passpoint.domain.fcmtoken.service.FcmTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * FCM 기기 토큰 등록/삭제 API
 */
@Tag(name = "FcmToken", description = "FCM 기기 토큰 관리 API")
@RestController
@RequiredArgsConstructor
public class FcmTokenController {

    private final FcmTokenService fcmTokenService;

    @Operation(summary = "FCM 토큰 등록", description = "기기 토큰을 등록한다. 같은 토큰 재등록 시 멱등 처리, 다른 계정이 쓰던 토큰이면 소유자를 현재 사용자로 변경한다.")
    @PostMapping("/api/v1/users/me/fcm-token")
    public ResponseEntity<FcmTokenResponse> registerToken(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody FcmTokenRegisterRequest request) {
        return ResponseEntity.ok(fcmTokenService.registerToken(userId, request.token()));
    }

    @Operation(summary = "FCM 토큰 삭제", description = "기기 토큰을 삭제한다. 본인 소유 토큰이 아니면 404 FCM_TOKEN_NOT_FOUND.")
    @DeleteMapping("/api/v1/users/me/fcm-token/{tokenId}")
    public ResponseEntity<Void> deleteToken(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long tokenId) {
        fcmTokenService.deleteToken(userId, tokenId);
        return ResponseEntity.noContent().build();
    }
}
