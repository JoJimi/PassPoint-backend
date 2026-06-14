package org.example.passpoint.domain.feedback.service;

import org.example.passpoint.domain.feedback.dto.FeedbackResult;
import org.example.passpoint.domain.question.entity.Question;

/**
 * 답변에 대한 AI 피드백 생성기
 * - 2주차: DummyFeedbackGenerator (고정 JSON)
 * - Step 2: SpringAiFeedbackGenerator로 교체 (인터페이스는 그대로)
 */
public interface FeedbackGenerator {

    FeedbackResult generate(Question question, String answerText);
}
