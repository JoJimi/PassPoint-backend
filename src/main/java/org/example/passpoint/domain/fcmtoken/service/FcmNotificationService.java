package org.example.passpoint.domain.fcmtoken.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * FCM 푸시 발송
 * - Firebase 호출(외부 I/O)이라 트랜잭션을 걸지 않는다. 토큰 조회/정리는 FcmTokenService 쪼개진 트랜잭션에서 처리.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FcmNotificationService {

    private static final Set<MessagingErrorCode> INVALID_TOKEN_ERRORS =
            Set.of(MessagingErrorCode.INVALID_ARGUMENT, MessagingErrorCode.UNREGISTERED);

    private final FirebaseMessaging firebaseMessaging;
    private final FcmTokenService fcmTokenService;

    public void sendPushNotification(Long userId, Long answerId, String title, String body) {
        List<String> tokens = fcmTokenService.getFcmTokensByUserId(userId);
        for (String token : tokens) {
            send(token, answerId, title, body);
        }
    }

    private void send(String token, Long answerId, String title, String body) {
        Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putData("answerId", String.valueOf(answerId))
                .putData("type", "feedback_completed")
                .build();

        try {
            firebaseMessaging.send(message);
            log.info("FCM sent successfully: answerId={}", answerId);
        } catch (FirebaseMessagingException e) {
            log.warn("FCM notification error: answerId={}, errorCode={}", answerId, e.getMessagingErrorCode(), e);
            if (INVALID_TOKEN_ERRORS.contains(e.getMessagingErrorCode())) {
                fcmTokenService.deleteByToken(token);
            }
        }
    }
}
