package org.example.passpoint.domain.studylog.dto.response;

import java.time.LocalDate;

/**
 * 연속 학습일/오늘 학습량 조회 응답 (Redis 기반)
 */
public record StreakResponse(
        int currentStreak,
        int todaySolved,
        LocalDate lastStudyDate
) {
}
