package org.example.passpoint.domain.auth.client;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.example.passpoint.domain.auth.dto.response.GoogleUserInfo;
import org.example.passpoint.global.exception.auth.OAuthAuthenticationFailedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * 구글 ID 토큰을 검증하고 사용자 정보를 추출하는 컴포넌트
 * - 앱이 보낸 ID 토큰을 구글 공개키로 검증(서명·만료·audience 확인)
 * - 검증 성공 시 구글 고유 ID/이메일/이름을 꺼냄
 */
@Component
public class GoogleOAuthClient {

    private final GoogleIdTokenVerifier verifier;

    public GoogleOAuthClient(@Value("${google.client-id}") String clientId) {
        // 검증기 1회 생성 - audience(우리 clientId)로 "이 토큰이 우리 앱 것인지" 확인
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance()
        )
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    /** ID 토큰을 검증하고 사용자 정보를 반환 (실패 시 예외) */
    public GoogleUserInfo verify(String idTokenString) {
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                // 서명·만료·audience 불일치 등으로 검증 실패
                throw new OAuthAuthenticationFailedException();
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            return new GoogleUserInfo(
                    payload.getSubject(),           // 구글 고유 ID (sub)
                    payload.getEmail(),             // 이메일
                    (String) payload.get("name")    // 이름
            );
        } catch (OAuthAuthenticationFailedException e) {
            throw e;
        } catch (Exception e) {
            // 네트워크 오류, 파싱 오류 등
            throw new OAuthAuthenticationFailedException();
        }
    }
}
