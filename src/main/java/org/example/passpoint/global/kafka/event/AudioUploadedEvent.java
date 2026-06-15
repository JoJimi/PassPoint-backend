package org.example.passpoint.global.kafka.event;

/**
 * 음성 답변 접수 시 발행 (topic: audio.uploaded)
 * - STT Worker가 소비해 S3에서 음성을 다운로드하고 텍스트로 변환한다 (Step B4)
 */
public record AudioUploadedEvent(Long answerId) {
}
