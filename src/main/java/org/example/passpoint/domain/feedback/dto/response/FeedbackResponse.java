package org.example.passpoint.domain.feedback.dto.response;

import org.example.passpoint.domain.feedback.entity.Feedback;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 피드백 응답
 * - 답변 상세 조회(GET /answers/{id})에 임베드되거나, 피드백 단독 조회에 사용된다
 */
public record FeedbackResponse(
        Integer score,
        Integer accuracyScore,
        Integer structureScore,
        Integer completenessScore,
        List<String> goodPoints,
        List<String> improvementPoints,
        List<String> coveredKeywords,
        LocalDateTime createdAt
) {
    public static FeedbackResponse from(Feedback feedback) {
        return new FeedbackResponse(
                feedback.getScore(),
                feedback.getAccuracyScore(),
                feedback.getStructureScore(),
                feedback.getCompletenessScore(),
                feedback.getGoodPoints(),
                feedback.getImprovementPoints(),
                feedback.getCoveredKeywords(),
                feedback.getCreatedAt()
        );
    }
}
