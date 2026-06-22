package org.example.passpoint.global.exception.user;

import org.example.passpoint.global.exception.BusinessException;
import org.example.passpoint.global.exception.ErrorCode;

/** 이메일이 이미 다른 계정(이메일/구글/카카오 등 provider 불문)에서 사용 중일 때 - 회원가입/소셜 신규가입 모두에서 발생 */
public class DuplicateEmailException extends BusinessException {
    public DuplicateEmailException() {
        super(ErrorCode.DUPLICATE_EMAIL);
    }
}
