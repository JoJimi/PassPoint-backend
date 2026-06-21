package org.example.passpoint.domain.user.entity;


/**
 * 가입 경로
 * - GOOGLE: 구글 OAuth
 * - KAKAO: 카카오 OAuth
 * - EMAIL: 이메일/비밀번호 자체 가입 (oauthId에는 email을 그대로 저장해 기존 (provider, oauthId) 유니크 제약을 재사용)
 */
public enum OAuthProvider {
    GOOGLE,
    KAKAO,
    EMAIL
}
