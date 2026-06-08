package org.example.passpoint.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.passpoint.domain.auth.dto.request.GoogleLoginRequest;
import org.example.passpoint.domain.auth.dto.request.RefreshRequest;
import org.example.passpoint.domain.auth.dto.response.TokenResponse;
import org.example.passpoint.domain.auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 관련 API
 * - 소셜 로그인, 토큰 갱신, 로그아웃 처리
 */
@Tag(name = "Auth", description = "사용자 인증 관련 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 구글 로그인 - 앱이 보낸 ID 토큰으로 인증하고 JWT 발급 */
    @Operation(summary = "구글 로그인", description = "구글 ID 토큰을 받아 로그인/가입 후 JWT를 발급한다.")
    @PostMapping("/login/google")
    public ResponseEntity<TokenResponse> loginWithGoogle(
            @Valid @RequestBody GoogleLoginRequest request){

        TokenResponse response = authService.loginWithGoogle(request.idToken());
        return ResponseEntity.ok(response);
    }

    /** 토큰 갱신 - refreshToken으로 Access/Refresh를 재발급한다. */
    @Operation(summary = "토큰 갱신", description = "refreshToken으로 Access/Refresh 토큰을 재발급한다.")
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh (
            @Valid @RequestBody RefreshRequest request) {

        return ResponseEntity.ok(authService.reissue(request.refreshToken()));
    }

    /** 로그아웃 - 현재 토큰 무효화 */
    @Operation(summary = "로그아웃", description = "refresh 토큰을 삭제하고 access 토큰을 블랙리스트에 등록한다.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        String accessToken = (String) authentication.getCredentials();      // 필터가 넣어둔 토큰

        authService.logout(userId, accessToken);
        return ResponseEntity.noContent().build();      // 204
    }

}
