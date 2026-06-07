package org.example.passpoint.global.exception.user;

import org.example.passpoint.global.exception.BusinessException;
import org.example.passpoint.global.exception.ErrorCode;

/** 요청한 사용자를 DB에서 찾을 수 없을 때 던지는 예외 */
public class UserNotFoundException extends BusinessException {
    public UserNotFoundException() {
        super(ErrorCode.USER_NOT_FOUND);
    }
}
