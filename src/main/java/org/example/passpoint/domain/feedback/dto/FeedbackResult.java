package org.example.passpoint.domain.feedback.dto;

import java.util.List;

/**
 * FeedbackGenerator의 생성 결과
 * - LLM(또는 더미)이 채점한 결과를 담는 내부 전달용 DTO
 */
public record FeedbackResult(
        Integer score,
        Integer accuracyScore,
        Integer structureScore,
        Integer completenessScore,
        List<String> goodPoints,
        List<String> improvementPoints,
        List<String> coveredKeywords
) {
}
