package org.example.passpoint.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.passpoint.domain.user.dto.UserResponse;
import org.example.passpoint.domain.user.dto.UserStatsResponse;
import org.example.passpoint.domain.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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

    /** 홈 대시보드용 학습 통계 (연속 학습일, 총 답변 수, 평균/최고 점수) */
    @Operation(summary = "내 학습 통계", description = "연속 학습일(Redis), 총 답변 수, 평균/최고 점수(RDB)를 조회한다.")
    @GetMapping("/me/stats")
    public UserStatsResponse getMyStats(
            @AuthenticationPrincipal Long userId) {
        return userService.getMyStats(userId);
    }
}
