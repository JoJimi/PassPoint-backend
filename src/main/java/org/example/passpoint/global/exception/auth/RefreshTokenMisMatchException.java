package org.example.passpoint.global.exception.auth;

import org.example.passpoint.global.exception.BusinessException;
import org.example.passpoint.global.exception.ErrorCode;

/** Redis에 저장된 리프레시 토큰과 일치하지 않을 때 (재사용 의심 포함) */
public class RefreshTokenMisMatchException extends BusinessException {
    public RefreshTokenMisMatchException() { super(ErrorCode.REFRESH_TOKEN_MISMATCH); }
}
