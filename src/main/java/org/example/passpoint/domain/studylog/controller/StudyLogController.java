package org.example.passpoint.domain.studylog.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.passpoint.domain.studylog.dto.response.StreakResponse;
import org.example.passpoint.domain.studylog.service.StudyLogService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 연속 학습일(스트릭) 조회 API
 */
@Tag(name = "StudyLog", description = "연속 학습일(스트릭) API")
@RestController
@RequiredArgsConstructor
public class StudyLogController {

    private final StudyLogService studyLogService;

    /** 연속 학습일 + 오늘 풀이 수 조회 (Redis) */
    @Operation(summary = "스트릭 조회", description = "연속 학습일, 오늘 풀이 수, 마지막 학습일을 조회한다.")
    @GetMapping("/api/v1/study-logs/streak")
    public StreakResponse getStreak(@AuthenticationPrincipal Long userId) {
        return studyLogService.getStreak(userId);
    }
}
