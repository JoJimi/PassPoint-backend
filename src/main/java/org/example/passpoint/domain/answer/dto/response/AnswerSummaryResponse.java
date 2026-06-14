package org.example.passpoint.domain.answer.dto.response;

import org.example.passpoint.domain.answer.entity.Answer;

import java.time.LocalDateTime;

/**
 * 답변 목록(이력) 응답
 * - GET /api/v1/answers, GET /api/v1/questions/{id}/answers 에서 공용으로 사용
 */
public record AnswerSummaryResponse(
        Long answerId,
        String questionTitle,
        String mainCategory,
        String status,
        Integer score,
        LocalDateTime createdAt
) {
    public static AnswerSummaryResponse of(Answer answer, Integer score) {
        return new AnswerSummaryResponse(
                answer.getId(),
                answer.getQuestion().getTitle(),
                answer.getQuestion().getMainCategory().name(),
                answer.getStatus().name(),
                score,
                answer.getCreatedAt()
        );
    }
}
