package org.example.passpoint.global.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml의 jwt.* 설정값을 바인딩하는 프로퍼티 클래스
 * - secret: 토큰 서명에 쓰는 비밀키
 * - accessTokenValidity / refreshTokenValidity: 각 토큰 유효기간(밀리초)
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long accessTokenValidity,
        long refreshTokenValidity
) {
}
