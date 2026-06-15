package org.example.passpoint.global.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * 3주차 비동기 파이프라인에서 사용하는 토픽 정의
 * - 로컬 단일 브로커 기준 partitions=1, replicas=1
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic audioUploadedTopic() {
        return TopicBuilder.name(KafkaTopics.AUDIO_UPLOADED).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic audioTranscribedTopic() {
        return TopicBuilder.name(KafkaTopics.AUDIO_TRANSCRIBED).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic feedbackRequestedTopic() {
        return TopicBuilder.name(KafkaTopics.FEEDBACK_REQUESTED).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic feedbackCompletedTopic() {
        return TopicBuilder.name(KafkaTopics.FEEDBACK_COMPLETED).partitions(1).replicas(1).build();
    }
}
