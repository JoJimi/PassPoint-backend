package org.example.passpoint.global.exception.auth;

import org.example.passpoint.global.exception.BusinessException;
import org.example.passpoint.global.exception.ErrorCode;

/** 구글/카카오 등 소셜 OAuth 인증에 실패했을 때 던지는 예외 */
public class OAuthAuthenticationFailedException extends BusinessException {
    public OAuthAuthenticationFailedException() {
        super(ErrorCode.OAUTH_AUTHENTICATION_FAILED);
    }
}