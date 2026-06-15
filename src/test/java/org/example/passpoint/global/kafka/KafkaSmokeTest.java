package org.example.passpoint.global.kafka;

import lombok.extern.slf4j.Slf4j;
import org.example.passpoint.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kafka 발행->수신이 실제로 도는지 확인하는 스모크 테스트 (B1)
 * - 더미 이벤트를 발행하고, 더미 컨슈머가 이를 수신해 로그를 남기는지 확인한다
 */
@Slf4j
@Import({TestcontainersConfiguration.class, KafkaSmokeTest.DummyConsumer.class})
@SpringBootTest
class KafkaSmokeTest {

    private static final String SMOKE_TOPIC = "smoke.test";

    @Autowired
    private KafkaEventPublisher kafkaEventPublisher;

    @Autowired
    private DummyConsumer dummyConsumer;

    @Test
    void 이벤트를_발행하면_더미컨슈머가_수신한다() throws InterruptedException {
        kafkaEventPublisher.publish(SMOKE_TOPIC, "1", new DummyEvent("hello-kafka"));

        boolean received = dummyConsumer.awaitMessage(10, TimeUnit.SECONDS);

        assertThat(received).isTrue();
        assertThat(dummyConsumer.lastMessage()).isEqualTo("hello-kafka");
    }

    public record DummyEvent(String message) {
    }

    @Slf4j
    @Component
    public static class DummyConsumer {

        private final CountDownLatch latch = new CountDownLatch(1);
        private final AtomicReference<String> lastMessage = new AtomicReference<>();

        @KafkaListener(topics = SMOKE_TOPIC)
        public void onMessage(DummyEvent event) {
            log.info("스모크 테스트 이벤트 수신: {}", event);
            lastMessage.set(event.message());
            latch.countDown();
        }

        public boolean awaitMessage(long timeout, TimeUnit unit) throws InterruptedException {
            return latch.await(timeout, unit);
        }

        public String lastMessage() {
            return lastMessage.get();
        }
    }
}
