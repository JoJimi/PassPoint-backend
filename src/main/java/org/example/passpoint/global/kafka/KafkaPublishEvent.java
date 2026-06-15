package org.example.passpoint.global.kafka;

/**
 * DB 트랜잭션 커밋 후 Kafka로 발행할 이벤트를 담는 Spring 애플리케이션 이벤트
 * - 서비스 계층은 @Transactional 메서드 안에서 이 이벤트만 publishEvent()로 발행한다
 * - 실제 Kafka 발행은 AfterCommitKafkaPublisher가 AFTER_COMMIT 시점에 수행한다
 *   (DB 커밋과 Kafka 발행을 한 트랜잭션으로 묶을 수 없는 dual-write 문제에 대한 실용적 대응)
 */
public record KafkaPublishEvent(String topic, String key, Object payload) {
}
