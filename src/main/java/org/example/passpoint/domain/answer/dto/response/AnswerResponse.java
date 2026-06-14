package org.example.passpoint.domain.answer.dto.response;

import org.example.passpoint.domain.answer.entity.Answer;
import org.example.passpoint.domain.answer.entity.AnswerStatus;

import java.time.LocalDateTime;

/**
 * 답변 제출 응답 (POST /api/v1/answers)
 * - 2주차는 동기 처리라 피드백까지 끝낸 최종 status(DONE/FAILED)를 바로 반환한다
 */
public record AnswerResponse(
        Long answerId,
        Long questionId,
        String type,
        String status,
        LocalDateTime createdAt
) {
    public static AnswerResponse of(Answer answer, AnswerStatus status) {
        return new AnswerResponse(
                answer.getId(),
                answer.getQuestion().getId(),
                answer.getType().name(),
                status.name(),
                answer.getCreatedAt()
        );
    }
}
