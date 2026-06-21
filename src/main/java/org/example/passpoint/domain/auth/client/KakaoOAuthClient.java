package org.example.passpoint.domain.auth.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.passpoint.domain.auth.dto.response.KakaoUserInfo;
import org.example.passpoint.global.exception.auth.OAuthAuthenticationFailedException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 카카오 액세스 토큰으로 사용자 정보를 조회하는 컴포넌트
 * - 안드로이드 카카오 SDK가 발급한 액세스 토큰을 받아 카카오 서버(kapi.kakao.com)에 직접 물어봄
 * - 구글과 달리 카카오 액세스 토큰은 서명 검증 가능한 JWT가 아니므로, 토큰 검증 자체를 "/v2/user/me" 호출 성공 여부로 대신한다
 */
@Component
public class KakaoOAuthClient {

    private static final String USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";

    private final RestClient restClient = RestClient.create();

    /** 액세스 토큰으로 카카오 사용자 정보를 조회 (실패 시 예외) */
    public KakaoUserInfo getUserInfo(String accessToken) {
        try {
            KakaoUserResponse response = restClient.get()
                    .uri(USER_INFO_URI)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(KakaoUserResponse.class);

            if (response == null) {
                throw new OAuthAuthenticationFailedException();
            }

            String email = response.kakaoAccount() != null ? response.kakaoAccount().email() : null;
            String nickname = response.kakaoAccount() != null && response.kakaoAccount().profile() != null
                    ? response.kakaoAccount().profile().nickname()
                    : null;

            return new KakaoUserInfo(String.valueOf(response.id()), email, nickname);
        } catch (OAuthAuthenticationFailedException e) {
            throw e;
        } catch (RestClientException e) {
            // 토큰 만료/위조로 401, 네트워크 오류 등
            throw new OAuthAuthenticationFailedException();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KakaoUserResponse(Long id, @JsonProperty("kakao_account") KakaoAccount kakaoAccount) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KakaoAccount(String email, Profile profile) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Profile(String nickname) {
    }
}
