package org.example.passpoint.global.exception.auth;

import org.example.passpoint.global.exception.BusinessException;
import org.example.passpoint.global.exception.ErrorCode;

public class OAuthAuthenticationFailedException extends BusinessException {
    public OAuthAuthenticationFailedException() {
        super(ErrorCode.OAUTH_AUTHENTICATION_FAILED);
    }
}