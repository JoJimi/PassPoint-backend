package org.example.passpoint.global.exception.answer;

import org.example.passpoint.global.exception.BusinessException;
import org.example.passpoint.global.exception.ErrorCode;

/** 음성 답변(type=VOICE)은 아직 지원하지 않을 때 (3주차 예정) */
public class VoiceNotSupportedException extends BusinessException {
    public VoiceNotSupportedException() {
        super(ErrorCode.VOICE_NOT_SUPPORTED_YET);
    }
}
