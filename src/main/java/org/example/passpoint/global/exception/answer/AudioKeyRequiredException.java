package org.example.passpoint.global.exception.answer;

import org.example.passpoint.global.exception.BusinessException;
import org.example.passpoint.global.exception.ErrorCode;

public class AudioKeyRequiredException extends BusinessException {
    public AudioKeyRequiredException() {
        super(ErrorCode.AUDIO_KEY_REQUIRED);
    }
}
