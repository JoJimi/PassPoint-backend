package org.example.passpoint.domain.question.service;

import lombok.RequiredArgsConstructor;
import org.example.passpoint.domain.bookmark.repository.UserBookmarkRepository;
import org.example.passpoint.domain.question.document.QuestionDocument;
import org.example.passpoint.domain.question.dto.response.QuestionSearchResponse;
import org.example.passpoint.domain.question.repository.search.QuestionSearchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionSearchService {

    private final QuestionSearchRepository questionSearchRepository;
    private final UserBookmarkRepository bookmarkRepository;

    /** 키워드로 질문 검색 (Nori 형태소 매칭) */
    public Page<QuestionSearchResponse> search(Long userId, String keyword, String mainCategory, Pageable pageable) {
        Page<QuestionDocument> documents = questionSearchRepository.searchQuestions(keyword, mainCategory, pageable);

        List<Long> questionIds = documents.getContent().stream()
                .map(QuestionDocument::getId)
                .collect(Collectors.toList());
        Set<Long> bookmarkedIds = questionIds.isEmpty()
                ? Collections.emptySet()
                : bookmarkRepository.findBookmarkedQuestionIds(userId, questionIds);

        return documents.map(document -> QuestionSearchResponse.from(document, bookmarkedIds.contains(document.getId())));
    }
}
