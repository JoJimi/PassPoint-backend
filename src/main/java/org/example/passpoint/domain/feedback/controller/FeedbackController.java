package org.example.passpoint.domain.feedback.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.passpoint.domain.feedback.dto.response.FeedbackResponse;
import org.example.passpoint.domain.feedback.service.FeedbackService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 피드백 단독 조회 API
 */
@Tag(name = "Feedback", description = "AI 피드백 단독 조회 API")
@RestController
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    /** 답변에 대한 AI 피드백 단독 조회 */
    @Operation(summary = "피드백 단독 조회", description = "답변에 대한 AI 피드백을 조회한다. status != DONE이면 404 FEEDBACK_NOT_READY.")
    @GetMapping("/api/v1/feedbacks/{answerId}")
    public FeedbackResponse getFeedback(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long answerId) {
        return feedbackService.getFeedback(userId, answerId);
    }
}
