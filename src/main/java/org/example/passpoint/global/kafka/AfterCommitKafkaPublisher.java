package org.example.passpoint.global.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * KafkaPublishEvent를 DB 트랜잭션 커밋 완료(AFTER_COMMIT) 후에 Kafka로 발행한다
 */
@Component
@RequiredArgsConstructor
public class AfterCommitKafkaPublisher {

    private final KafkaEventPublisher kafkaEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAfterCommit(KafkaPublishEvent event) {
        kafkaEventPublisher.publish(event.topic(), event.key(), event.payload());
    }
}
