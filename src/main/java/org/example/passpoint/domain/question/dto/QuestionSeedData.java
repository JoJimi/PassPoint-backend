package org.example.passpoint.domain.question.dto;

import org.example.passpoint.domain.question.entity.Difficulty;
import org.example.passpoint.domain.question.entity.SubCategory;

import java.util.List;

/**
 * questions.json의 질문 한 건을 매핑하는 시드 데이터 DTO
 */
public record QuestionSeedData(
        SubCategory subCategory,
        Difficulty difficulty,
        String title,
        String content,
        String hint,
        List<String> guidePoints,
        List<String> keywordPool,
        String modelAnswer
) {
}
