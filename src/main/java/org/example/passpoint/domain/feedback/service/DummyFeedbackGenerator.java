package org.example.passpoint.domain.feedback.service;

import org.example.passpoint.domain.feedback.dto.FeedbackResult;
import org.example.passpoint.domain.question.entity.Question;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 더미 피드백 생성기
 * - 2주차 동기 흐름을 관통시키기 위한 임시 구현, 항상 고정된 JSON을 반환한다
 * - Step 2에서 SpringAiFeedbackGenerator로 교체
 */
@Component
public class DummyFeedbackGenerator implements FeedbackGenerator {

    @Override
    public FeedbackResult generate(Question question, String answerText) {
        return new FeedbackResult(
                80,
                80,
                80,
                80,
                List.of("핵심 개념을 잘 짚었어요."),
                List.of("구체적인 예시를 들어보면 더 좋아요."),
                List.of()
        );
    }
}
