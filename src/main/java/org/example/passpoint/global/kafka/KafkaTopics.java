package org.example.passpoint.global.kafka;

/**
 * Kafka 토픽 이름 상수
 * - audio.uploaded: 음성 답변 접수 시 발행 (STT Worker가 소비)
 * - audio.transcribed: STT 완료 시 발행 (feedback.requested로 이어짐)
 * - feedback.requested: 텍스트 접수 / 음성 STT 후 발행 (Feedback Worker가 소비) - 텍스트·음성 합류 지점
 * - feedback.completed: 피드백 완료 시 발행 (폴링/FCM용 알림)
 */
public final class KafkaTopics {

    public static final String AUDIO_UPLOADED = "audio.uploaded";
    public static final String AUDIO_TRANSCRIBED = "audio.transcribed";
    public static final String FEEDBACK_REQUESTED = "feedback.requested";
    public static final String FEEDBACK_COMPLETED = "feedback.completed";

    private KafkaTopics() {
    }
}
