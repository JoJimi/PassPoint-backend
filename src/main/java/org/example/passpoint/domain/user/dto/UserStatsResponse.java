package org.example.passpoint.domain.user.dto;

/**
 * 사용자 학습 통계 응답 (홈 대시보드 카드용)
 */
public record UserStatsResponse(
        int currentStreak,
        long totalAnswered,
        int averageScore,
        int bestScore
) {
}
