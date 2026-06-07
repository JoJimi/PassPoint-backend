package org.example.passpoint.global.exception.auth;

import org.example.passpoint.global.exception.BusinessException;
import org.example.passpoint.global.exception.ErrorCode;

/** 토큰이 만료됐을 때 던지는 예외 */
public class ExpiredTokenException extends BusinessException {
    public ExpiredTokenException() {
        super(ErrorCode.EXPIRED_TOKEN);
    }
}