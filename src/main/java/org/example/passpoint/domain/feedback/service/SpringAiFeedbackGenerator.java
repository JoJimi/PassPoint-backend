package org.example.passpoint.domain.feedback.service;

import org.example.passpoint.domain.feedback.dto.FeedbackResult;
import org.example.passpoint.domain.feedback.dto.LlmFeedbackResult;
import org.example.passpoint.domain.question.entity.Question;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Spring AI(OpenAI) 기반 피드백 생성기
 * - 점수(accuracy/structure/completeness)와 good/improvement points, coveredKeywords를 LLM에 구조화 출력으로 요청
 * - score(총점)는 세부 점수 평균으로 서버에서 계산
 * - modelAnswer는 Question에 이미 저장된 값을 그대로 사용 (LLM에 요청하지 않음)
 * - coveredKeywords는 question.keywordPool과의 교집합만 남겨 환각을 방지
 */
@Component
public class SpringAiFeedbackGenerator implements FeedbackGenerator {

    private final ChatClient chatClient;
    private final BeanOutputConverter<LlmFeedbackResult> outputConverter =
            new BeanOutputConverter<>(LlmFeedbackResult.class);

    public SpringAiFeedbackGenerator(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public FeedbackResult generate(Question question, String answerText) {
        String response = chatClient.prompt()
                .user(buildPrompt(question, answerText))
                .options(OpenAiChatOptions.builder()
                        .responseFormat(OpenAiChatModel.ResponseFormat.builder()
                                .type(OpenAiChatModel.ResponseFormat.Type.JSON_SCHEMA)
                                .jsonSchema(outputConverter.getJsonSchema())
                                .build()))
                .call()
                .content();

        if (response == null) {
            throw new IllegalStateException("LLM 응답이 비어있습니다.");
        }

        LlmFeedbackResult result = outputConverter.convert(response);

        return new FeedbackResult(
                computeScore(result),
                result.accuracyScore(),
                result.structureScore(),
                result.completenessScore(),
                result.goodPoints(),
                result.improvementPoints(),
                filterKeywords(result.coveredKeywords(), question.getKeywordPool())
        );
    }

    private int computeScore(LlmFeedbackResult result) {
        return Math.round((result.accuracyScore() + result.structureScore() + result.completenessScore()) / 3.0f);
    }

    /** LLM이 반환한 키워드를 question.keywordPool과 교집합만 남긴다 (pool에 없는 키워드는 버림) */
    List<String> filterKeywords(List<String> covered, List<String> keywordPool) {
        if (covered == null || keywordPool == null) {
            return List.of();
        }

        Map<String, String> poolByLowerCase = new LinkedHashMap<>();
        for (String keyword : keywordPool) {
            poolByLowerCase.putIfAbsent(keyword.trim().toLowerCase(), keyword);
        }

        return covered.stream()
                .filter(Objects::nonNull)
                .map(keyword -> poolByLowerCase.get(keyword.trim().toLowerCase()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private String buildPrompt(Question question, String answerText) {
        return """
                너는 IT 기술 면접 답변을 채점하는 평가자야. 아래 질문, 채점 기준, 사용자의 답변을 보고 JSON으로 평가 결과를 작성해.

                [질문 제목]
                %s

                [질문 설명]
                %s

                [모범 답안 / 채점 루브릭]
                %s

                [인정 키워드 목록]
                %s

                [사용자 답변]
                %s

                [평가 기준]
                - accuracyScore (0~100): 기술적으로 정확한 내용을 담고 있는지
                - structureScore (0~100): 답변의 논리적 구성과 설명 흐름이 명확한지
                - completenessScore (0~100): 모범 답안/루브릭 대비 핵심 내용을 빠짐없이 다뤘는지
                - goodPoints: 답변에서 잘한 점 (한국어 문장 목록)
                - improvementPoints: 보완하면 좋을 점 (한국어 문장 목록)
                - coveredKeywords: 사용자 답변에서 실제로 언급된 키워드만, [인정 키워드 목록]에 있는 표현 그대로 골라 작성
                """.formatted(
                question.getTitle(),
                question.getContent(),
                question.getModelAnswer(),
                String.join(", ", question.getKeywordPool()),
                answerText
        );
    }
}
