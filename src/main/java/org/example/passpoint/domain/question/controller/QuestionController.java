package org.example.passpoint.domain.question.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.passpoint.domain.question.dto.response.QuestionDetailResponse;
import org.example.passpoint.domain.question.dto.response.QuestionSearchResponse;
import org.example.passpoint.domain.question.service.QuestionSearchService;
import org.example.passpoint.domain.question.service.QuestionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

/**
 * 질문 검색·조회 API
 */
@Tag(name = "Question", description = "질문 검색·조회 API")
@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionSearchService questionSearchService;
    private final QuestionService questionService;

    /** 질문 상세 조회 */
    @Operation(summary = "질문 상세", description = "질문 하나의 상세 정보를 조회한다 (정답류 제외).")
    @GetMapping("/{id}")
    public QuestionDetailResponse getQuestionDetail(@PathVariable Long id) {
        return questionService.getQuestionDetail(id);
    }

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
