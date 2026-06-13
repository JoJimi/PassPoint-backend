package org.example.passpoint.domain.question.dto.response;

import org.example.passpoint.domain.question.document.QuestionDocument;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 질문 검색 결과 응답 (목록용)
 * - 정답·채점용 데이터(modelAnswer, keywordPool 등)는 제외
 */
public record QuestionSearchResponse(
        Long id,
        String title,
        String mainCategory,
        String subCategory,
        String difficulty,
        List<String> tags,
        LocalDateTime createdAt
) {
    public static QuestionSearchResponse from(QuestionDocument document) {
        return new QuestionSearchResponse(
                document.getId(),
                document.getTitle(),
                document.getMainCategory(),
                document.getSubCategory(),
                document.getDifficulty(),
                document.getTags(),
                document.getCreatedAt()
        );
    }
}
