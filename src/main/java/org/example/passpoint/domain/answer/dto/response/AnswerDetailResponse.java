package org.example.passpoint.domain.answer.dto.response;

import org.example.passpoint.domain.answer.entity.Answer;
import org.example.passpoint.domain.feedback.dto.response.FeedbackResponse;
import org.example.passpoint.domain.question.entity.Question;

import java.time.LocalDateTime;

/**
 * 답변 단건 조회 응답 (GET /api/v1/answers/{id})
 * - 3주차 폴링이 그대로 사용할 응답 모양: status != DONE이면 feedback은 null
 */
public record AnswerDetailResponse(
        Long answerId,
        QuestionInfo question,
        String type,
        String answerText,
        String status,
        FeedbackResponse feedback,
        LocalDateTime createdAt
) {
    public record QuestionInfo(
            Long id,
            String title,
            String mainCategory
    ) {
        public static QuestionInfo from(Question question) {
            return new QuestionInfo(
                    question.getId(),
                    question.getTitle(),
                    question.getMainCategory().name()
            );
        }
    }

    public static AnswerDetailResponse of(Answer answer, FeedbackResponse feedback) {
        return new AnswerDetailResponse(
                answer.getId(),
                QuestionInfo.from(answer.getQuestion()),
                answer.getType().name(),
                answer.getAnswerText(),
                answer.getStatus().name(),
                feedback,
                answer.getCreatedAt()
        );
    }
}
