package org.example.passpoint.domain.question.service;

import lombok.RequiredArgsConstructor;
import org.example.passpoint.domain.question.dto.response.QuestionSearchResponse;
import org.example.passpoint.domain.question.repository.QuestionSearchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QuestionSearchService {

    private final QuestionSearchRepository questionSearchRepository;

    /** 키워드로 질문 검색 (Nori 형태소 매칭) */
    public Page<QuestionSearchResponse> search(String keyword, Pageable pageable) {
        return questionSearchRepository.searchByKeyword(keyword, pageable)
                .map(QuestionSearchResponse::from);
    }
}
