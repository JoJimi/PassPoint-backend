package org.example.passpoint.global.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.example.passpoint.global.exception.auth.ExpiredTokenException;
import org.example.passpoint.global.exception.auth.InvalidTokenException;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;


/**
 * JWT 토큰의 생성·검증·정보 추출을 담당하는 컴포넌트
 * - Access/Refresh 토큰 발급
 * - 토큰에서 사용자 ID(subject) 추출
 * - 토큰 유효성(위조·만료) 검증
 */
@Component
public class JwtProvider {

    private final SecretKey key;
    private final long accessTokenValidity;
    private final long refreshTokenValidity;

    public JwtProvider (JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidity = properties.accessTokenValidity();
        this.refreshTokenValidity = properties.refreshTokenValidity();
    }

    /** Access Token 생성 */
    public String createAccessToken(Long userId) {
        return createToken(userId, accessTokenValidity);
    }

    /** Refresh Token 생성 */
    public String createRefreshToken(Long userId) {
        return createToken(userId, refreshTokenValidity);
    }

    /** 공통 토큰 생성 로직 - userId를 subject로, 만료시간을 설정해 서명 */
    private String createToken(Long userId, long validyMillis) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validyMillis);

        return Jwts.builder()
                .subject(String.valueOf(userId))    // 토큰 주인 = userId
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)                      // 비밀키로 서명
                .compact();
    }

    /** 토큰에서 userId 추출 (검증도 겸함 - 위조/만료면 예외) */
    public Long getUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    /** 토큰 유효성 검증 - 유효하면 true, 아니면 예외를 던짐 */
    public boolean validateToken(String token) {
        parseClaims(token); // 파싱 과정에서 위조·만료면 예외 발생
        return true;
    }

    /** 토큰을 파싱해 Claims(내용)를 꺼냄 - 검증 실패 시 우리 예외로 변환 */
    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)            // 같은 비밀키로 서명 검증
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new ExpiredTokenException();  // 만료
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException();  // 위조·형식오류 등
        }
    }
}
