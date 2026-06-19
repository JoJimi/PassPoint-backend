package org.example.passpoint.domain.fcmtoken.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.passpoint.domain.user.entity.User;
import org.example.passpoint.global.entity.BaseEntity;

/**
 * FCM 기기 토큰
 * - 한 사용자가 여러 기기 토큰을 가질 수 있다 (User 1:N FcmToken)
 * - token은 유니크: 같은 기기 토큰이 중복 저장될 수 없다
 */

@Getter
@Entity
@Table(name = "fcm_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FcmToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String token;

    @Builder
    private FcmToken(User user, String token) {
        this.user = user;
        this.token = token;
    }

    // 다른 계정이 쓰던 토큰을 같은 기기에서 재등록하는 경우 (재로그인) 소유자 변경
    public void reassignOwner(User user) {
        this.user = user;
    }
}