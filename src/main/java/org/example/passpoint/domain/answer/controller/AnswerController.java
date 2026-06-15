package org.example.passpoint.domain.answer.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.passpoint.domain.answer.dto.request.AnswerCreateRequest;
import org.example.passpoint.domain.answer.dto.response.AnswerDetailResponse;
import org.example.passpoint.domain.answer.dto.response.AnswerResponse;
import org.example.passpoint.domain.answer.dto.response.AnswerSummaryResponse;
import org.example.passpoint.domain.answer.service.AnswerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 답변 제출 및 이력 조회 API
 */
@Tag(name = "Answer", description = "답변 제출 및 이력 조회 API")
@RestController
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerService answerService;

    /** 답변 제출 (접수만 하고 즉시 202 반환, 피드백 생성은 FeedbackWorker가 비동기로 처리) */
    @Operation(summary = "답변 제출", description = "답변을 접수(status=PENDING)하고 즉시 202를 반환한다. 피드백은 비동기로 생성되며 GET /api/v1/answers/{id} 폴링으로 확인한다.")
    @PostMapping("/api/v1/answers")
    public ResponseEntity<AnswerResponse> submit(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody AnswerCreateRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(answerService.submit(userId, request));
    }

    /** 답변 단건 조회 (상태 + 피드백) */
    @Operation(summary = "답변 단건 조회", description = "답변의 상태와 피드백을 조회한다. status != DONE이면 feedback은 null.")
    @GetMapping("/api/v1/answers/{id}")
    public AnswerDetailResponse getAnswerDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        return answerService.getAnswerDetail(userId, id);
    }

    /** 내 답변 목록 (페이징) */
    @Operation(summary = "내 답변 목록", description = "내가 제출한 답변 이력을 페이징으로 조회한다.")
    @GetMapping("/api/v1/answers")
    public Page<AnswerSummaryResponse> getMyAnswers(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return answerService.getMyAnswers(userId, pageable);
    }

    /** 특정 질문에 대한 내 답변 이력 (페이징) */
    @Operation(summary = "질문별 내 답변 이력", description = "특정 질문에 대해 내가 제출한 답변 이력을 페이징으로 조회한다.")
    @GetMapping("/api/v1/questions/{questionId}/answers")
    public Page<AnswerSummaryResponse> getMyAnswersByQuestion(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long questionId,
            @PageableDefault(size = 20) Pageable pageable) {
        return answerService.getMyAnswersByQuestion(userId, questionId, pageable);
    }
}
