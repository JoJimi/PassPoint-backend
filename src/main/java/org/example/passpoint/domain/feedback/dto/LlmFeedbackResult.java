package org.example.passpoint.domain.feedback.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * LLM 구조화 출력 스키마
 * - 총점(score)은 서버에서 세부 점수로 계산하므로 LLM에 요청하지 않는다
 * - modelAnswer는 Question에 이미 저장돼 있으므로 LLM에 요청하지 않는다
 */
public record LlmFeedbackResult(
        @JsonProperty(required = true) Integer accuracyScore,
        @JsonProperty(required = true) Integer structureScore,
        @JsonProperty(required = true) Integer completenessScore,
        @JsonProperty(required = true) List<String> goodPoints,
        @JsonProperty(required = true) List<String> improvementPoints,
        @JsonProperty(required = true) List<String> coveredKeywords
) {
}
