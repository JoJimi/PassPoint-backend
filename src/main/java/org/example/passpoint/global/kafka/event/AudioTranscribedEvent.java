package org.example.passpoint.global.kafka.event;

/**
 * STT 변환 완료 시 발행 (topic: audio.transcribed)
 * - feedback.requested로 이어지는 bridge에서 사용한다 (Step B4)
 */
public record AudioTranscribedEvent(Long answerId) {
}
