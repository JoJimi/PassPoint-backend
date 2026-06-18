package org.example.passpoint.domain.question.dto.response;

import org.example.passpoint.domain.question.entity.Question;

/**
 * 질문 상세 응답
 * - 답변 전 화면용이라 정답류(modelAnswer, guidePoints, keywordPool)는 제외
 * - hint만 노출 (사용자 방향 힌트)
 */
public record QuestionDetailResponse(
        Long id,
        String mainCategory,
        String subCategory,
        String difficulty,
        String title,
        String content,
        String hint,
        boolean bookmarked
) {
    public static QuestionDetailResponse from(Question question, boolean bookmarked) {
        return new QuestionDetailResponse(
                question.getId(),
                question.getMainCategory().name(),
                question.getSubCategory().name(),
                question.getDifficulty().name(),
                question.getTitle(),
                question.getContent(),
                question.getHint(),
                bookmarked
        );
    }
}
