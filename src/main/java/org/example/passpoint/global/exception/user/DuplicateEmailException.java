package org.example.passpoint.global.exception.user;

import org.example.passpoint.global.exception.BusinessException;
import org.example.passpoint.global.exception.ErrorCode;

/** 이메일 회원가입 시 이미 같은 이메일로 가입된 계정이 있을 때 */
public class DuplicateEmailException extends BusinessException {
    public DuplicateEmailException() {
        super(ErrorCode.DUPLICATE_EMAIL);
    }
}
