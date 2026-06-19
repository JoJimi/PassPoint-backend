package org.example.passpoint.domain.fcmtoken.worker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.passpoint.domain.answer.entity.Answer;
import org.example.passpoint.domain.answer.repository.AnswerRepository;
import org.example.passpoint.domain.fcmtoken.service.FcmNotificationService;
import org.example.passpoint.global.exception.answer.AnswerNotFoundException;
import org.example.passpoint.global.kafka.KafkaTopics;
import org.example.passpoint.global.kafka.event.FeedbackCompletedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * feedback.completed 소비 -> 답변 작성자에게 FCM 푸시 발송
 * - FCM 전용 컨슈머 그룹(passpoint-fcm-group)으로 분리해 기존 폴링 흐름과 독립적으로 동작
 * - 발송 실패는 로그만 남기고 삼킨다 (한 건 실패가 컨슈머를 멈추면 이후 알림이 전부 막힌다)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeedbackCompletedConsumer {

    private final AnswerRepository answerRepository;
    private final FcmNotificationService fcmNotificationService;

    @KafkaListener(topics = KafkaTopics.FEEDBACK_COMPLETED, groupId = "passpoint-fcm-group")
    public void onFeedbackCompleted(FeedbackCompletedEvent event) {
        Long answerId = event.answerId();

        try {
            Answer answer = answerRepository.findByIdWithQuestionAndUser(answerId)
                    .orElseThrow(AnswerNotFoundException::new);

            fcmNotificationService.sendPushNotification(
                    answer.getUser().getId(),
                    answerId,
                    "피드백 완료 ✨",
                    answer.getQuestion().getTitle() + "에 대한 피드백이 준비됐습니다"
            );
        } catch (Exception e) {
            log.error("FCM 발송 처리 실패: answerId={}", answerId, e);
        }
    }
}
