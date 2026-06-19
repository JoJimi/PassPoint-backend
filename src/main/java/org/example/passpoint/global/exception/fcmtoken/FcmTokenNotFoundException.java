package org.example.passpoint.global.exception.fcmtoken;

import org.example.passpoint.global.exception.BusinessException;
import org.example.passpoint.global.exception.ErrorCode;

/** FCM 토큰을 찾을 수 없을 때 */
public class FcmTokenNotFoundException extends BusinessException {
    public FcmTokenNotFoundException() {
        super(ErrorCode.FCM_TOKEN_NOT_FOUND);
    }
}
