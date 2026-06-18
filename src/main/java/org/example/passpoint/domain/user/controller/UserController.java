package org.example.passpoint.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.passpoint.domain.user.dto.UserProfileUpdateRequest;
import org.example.passpoint.domain.user.dto.UserResponse;
import org.example.passpoint.domain.user.dto.UserStatsResponse;
import org.example.passpoint.domain.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 관련 API
 */
@Tag(name = "User", description = "사용자 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile(
            @AuthenticationPrincipal Long userId) {

        return ResponseEntity.ok(userService.getMyProfile(userId));
    }

    /** 프로필 수정 (닉네임, 상태 메시지) - 부분 수정: 보내지 않은 필드는 유지 */
    @Operation(summary = "프로필 수정", description = "닉네임, 상태 메시지를 수정한다. 보내지 않은 필드는 기존 값을 유지한다.")
    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateMyProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UserProfileUpdateRequest request) {
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }

    /** 홈 대시보드용 학습 통계 (연속 학습일, 총 답변 수, 평균/최고 점수) */
    @Operation(summary = "내 학습 통계", description = "연속 학습일(Redis), 총 답변 수, 평균/최고 점수(RDB)를 조회한다.")
    @GetMapping("/me/stats")
    public UserStatsResponse getMyStats(
            @AuthenticationPrincipal Long userId) {
        return userService.getMyStats(userId);
    }
}
