package org.example.passpoint.global.exception.auth;

import org.example.passpoint.global.exception.BusinessException;
import org.example.passpoint.global.exception.ErrorCode;

/** 토큰이 위조/손상 등으로 유효하지 않을 때 던지는 예외 */
public class InvalidTokenException extends BusinessException {
    public InvalidTokenException() {
        super(ErrorCode.INVALID_TOKEN);
    }
}
