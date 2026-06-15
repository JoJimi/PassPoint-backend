package org.example.passpoint.global.kafka.event;

/**
 * 답변의 텍스트가 준비되어 피드백 생성이 필요할 때 발행 (topic: feedback.requested)
 * - 텍스트 답변은 접수 즉시, 음성 답변은 STT 완료 후 발행된다 (텍스트·음성 합류 지점)
 * - FeedbackWorker가 소비한다
 */
public record FeedbackRequestedEvent(Long answerId) {
}
