package org.example.passpoint.domain.answer.dto.response;

import org.example.passpoint.domain.answer.entity.Answer;

import java.time.LocalDateTime;

/**
 * 답변 제출 응답 (POST /api/v1/answers)
 * - 3주차는 비동기 처리라 접수만 하고 status=PENDING으로 즉시 반환한다
 * - 이후 처리 결과는 GET /api/v1/answers/{id} 폴링으로 확인한다
 */
public record AnswerResponse(
        Long answerId,
        Long questionId,
        String type,
        String status,
        LocalDateTime createdAt
) {
    public static AnswerResponse of(Answer answer) {
        return new AnswerResponse(
                answer.getId(),
                answer.getQuestion().getId(),
                answer.getType().name(),
                answer.getStatus().name(),
                answer.getCreatedAt()
        );
    }
}
