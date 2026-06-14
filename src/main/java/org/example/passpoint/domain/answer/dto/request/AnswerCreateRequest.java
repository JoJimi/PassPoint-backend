package org.example.passpoint.domain.answer.dto.request;

import jakarta.validation.constraints.NotNull;
import org.example.passpoint.domain.answer.entity.AnswerType;

/**
 * 답변 제출 요청
 * - answerText의 공백/길이 검증은 AnswerService에서 도메인 에러 코드로 처리한다
 */
public record AnswerCreateRequest(
        @NotNull Long questionId,
        @NotNull AnswerType type,
        String answerText
) {
}
