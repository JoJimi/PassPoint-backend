package org.example.passpoint.global.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka 이벤트 발행 공통 래퍼
 * - key는 answerId 등 식별자 문자열을 사용해 같은 답변의 이벤트가 같은 파티션으로 가도록 한다
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(String topic, String key, Object event) {
        kafkaTemplate.send(topic, key, event).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Kafka 발행 실패: topic={}, key={}, event={}", topic, key, event, ex);
            } else {
                log.info("Kafka 발행 성공: topic={}, key={}, event={}", topic, key, event);
            }
        });
    }
}
