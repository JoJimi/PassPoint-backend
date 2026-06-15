package org.example.passpoint.domain.feedback.worker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.passpoint.domain.answer.entity.Answer;
import org.example.passpoint.domain.answer.service.AnswerWriteService;
import org.example.passpoint.domain.feedback.dto.FeedbackResult;
import org.example.passpoint.domain.feedback.service.FeedbackGenerator;
import org.example.passpoint.global.kafka.KafkaTopics;
import org.example.passpoint.global.kafka.event.FeedbackRequestedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * feedback.requested 소비 -> LLM 피드백 생성 -> 결과 저장(DONE/FAILED)
 * - 텍스트 답변은 접수 즉시, 음성 답변은 STT 완료 후 이 토픽으로 합류한다
 * - tx1(상태 전이: PENDING -> ANALYZING) -> 트랜잭션 밖 LLM 호출 -> tx2(결과 저장 + DONE/FAILED)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeedbackWorker {

    private final AnswerWriteService answerWriteService;
    private final FeedbackGenerator feedbackGenerator;

    @KafkaListener(topics = KafkaTopics.FEEDBACK_REQUESTED)
    public void onFeedbackRequested(FeedbackRequestedEvent event) {
        Long answerId = event.answerId();

        // tx1: 멱등 체크 + status PENDING -> ANALYZING (question을 함께 fetch)
        Answer answer = answerWriteService.markAnalyzing(answerId);
        if (answer == null) {
            log.info("이미 처리된 답변이라 건너뜀: answerId={}", answerId);
            return;
        }

        try {
            // 트랜잭션 밖: LLM 호출
            FeedbackResult result = feedbackGenerator.generate(answer.getQuestion(), answer.getAnswerText());
            // tx2: 피드백 저장 + status = DONE
            answerWriteService.completeFeedback(answerId, result);
        } catch (Exception e) {
            log.error("피드백 생성 실패: answerId={}", answerId, e);
            // tx2: status = FAILED로 별도 마킹
            answerWriteService.markFailed(answerId);
        }
    }
}
