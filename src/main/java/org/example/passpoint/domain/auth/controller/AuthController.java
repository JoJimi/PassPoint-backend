package org.example.passpoint.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.passpoint.domain.auth.dto.request.GoogleLoginRequest;
import org.example.passpoint.domain.auth.dto.response.TokenResponse;
import org.example.passpoint.domain.auth.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
