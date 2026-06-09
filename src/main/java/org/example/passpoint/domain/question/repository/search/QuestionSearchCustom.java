package org.example.passpoint.domain.question.repository.search;

import org.example.passpoint.domain.question.document.QuestionDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 동적 조건 검색을 위한 커스텀 인터페이스
 * - 키워드/카테고리 유무에 따라 ES bool 쿼리를 동적 조립
 */
public interface QuestionSearchCustom {

    /**
     * 키워드 + 카테고리 동적 검색
     * @param keyword 검색어 (null/blank면 키워드 조건 제외)
     * @param mainCategory 대분류 필터 (null/blank면 카테고리 조건 제외)
     */
    Page<QuestionDocument> searchQuestions(String keyword, String mainCategory, Pageable pageable);
}
