package org.example.passpoint.global.kafka.event;

/**
 * 피드백 생성이 완료되어 답변이 DONE 상태가 되었을 때 발행 (topic: feedback.completed)
 * - 3주차에는 폴링이 DONE을 감지하므로 별도 소비자는 없다 (알림용으로 후순위 활용)
 */
public record FeedbackCompletedEvent(Long answerId) {
}
