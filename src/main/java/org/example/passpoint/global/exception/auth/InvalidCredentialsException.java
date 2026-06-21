package org.example.passpoint.global.exception.auth;

import org.example.passpoint.global.exception.BusinessException;
import org.example.passpoint.global.exception.ErrorCode;

/** 이메일 로그인 시 이메일/비밀번호가 일치하지 않을 때 (어느 쪽이 틀렸는지는 노출하지 않음) */
public class InvalidCredentialsException extends BusinessException {
    public InvalidCredentialsException() {
        super(ErrorCode.INVALID_CREDENTIALS);
    }
}
