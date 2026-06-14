package org.example.passpoint.domain.feedback.dto.response;

import org.example.passpoint.domain.answer.entity.Answer;
import org.example.passpoint.domain.feedback.entity.Feedback;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 피드백 응답
 * - 답변 상세 조회(GET /answers/{id})에 임베드되거나, 피드백 단독 조회(GET /feedbacks/{answerId})에 사용된다
 * - modelAnswer는 Feedback에 저장하지 않고 Question의 모범 답안을 그대로 노출한다
 */
public record FeedbackResponse(
        Long answerId,
        Integer score,
        Integer accuracyScore,
        Integer structureScore,
        Integer completenessScore,
        List<String> goodPoints,
        List<String> improvementPoints,
        String modelAnswer,
        List<String> coveredKeywords,
        LocalDateTime createdAt
) {
    public static FeedbackResponse from(Feedback feedback) {
        Answer answer = feedback.getAnswer();
        return new FeedbackResponse(
                answer.getId(),
                feedback.getScore(),
                feedback.getAccuracyScore(),
                feedback.getStructureScore(),
                feedback.getCompletenessScore(),
                feedback.getGoodPoints(),
                feedback.getImprovementPoints(),
                answer.getQuestion().getModelAnswer(),
                feedback.getCoveredKeywords(),
                feedback.getCreatedAt()
        );
    }
}
