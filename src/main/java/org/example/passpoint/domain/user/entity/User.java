package org.example.passpoint.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.passpoint.global.entity.BaseEntity;

/**
 * 사용자
 * - 소셜 로그인(google/kakao) 또는 이메일/비밀번호로 가입
 * - (oauth_provider, oauth_id) 조합은 유니크: 같은 계정으로 중복 가입할 수 없다
 * - EMAIL 가입의 경우 oauthId에 email을 그대로 저장해 동일한 유니크 제약을 그대로 활용한다 (별도 자격증명 테이블 없이 재사용)
 */

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(columnNames = {"oauth_provider", "oauth_id"})
)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "oauth_provider", nullable = false)
    private OAuthProvider oauthProvider;    // google | kakao | email

    @Column(name = "oauth_id", nullable = false)
    private String oauthId;                 // 소셜 측 고유 ID, EMAIL이면 email과 동일한 값

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String nickname;

    private String statusMessage;           // 한 줄 소개 (nullable)

    private String password;                // EMAIL 가입자만 사용 (BCrypt 해시). 소셜 가입자는 null

    @Builder
    private User(OAuthProvider oauthProvider, String oauthId, String email, String nickname, String statusMessage, String password) {
        this.oauthProvider = oauthProvider;
        this.oauthId = oauthId;
        this.email = email;
        this.nickname = nickname;
        this.statusMessage = statusMessage;
        this.password = password;
    }

    public void updateProfile(String nickname, String statusMessage) {
        if (nickname != null) this.nickname = nickname;
        if (statusMessage != null) this.statusMessage = statusMessage;
    }
}
