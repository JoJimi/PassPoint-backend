package org.example.passpoint.domain.user.repository;

import org.example.passpoint.domain.user.entity.OAuthProvider;
import org.example.passpoint.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * User 엔티티 영속성 처리 리포지토리
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** 소셜 제공자 + 소셜 ID로 가입된 사용자 조회 (로그인/가입 분기에 사용) */
    Optional<User> findByOauthProviderAndOauthId(OAuthProvider oAuthProvider, String oauthId);

}
