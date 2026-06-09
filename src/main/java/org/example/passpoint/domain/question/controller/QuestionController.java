package org.example.passpoint.domain.question.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.passpoint.domain.question.dto.response.QuestionSearchResponse;
import org.example.passpoint.domain.question.service.QuestionSearchService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 질문 검색·조회 API
 */
@Tag(name = "Question", description = "질문 검색·조회 API")
@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionSearchService questionSearchService;

    /** 키워드로 질문 검색 */
    @Operation(summary = "질문 검색", description = "키워드로 질문을 검색한다. (ElasticSearch + Nori).")
    @GetMapping
    public Page<QuestionSearchResponse> search (
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            Pageable pageable) {
        return questionSearchService.search(keyword, category, pageable);
    }
}
