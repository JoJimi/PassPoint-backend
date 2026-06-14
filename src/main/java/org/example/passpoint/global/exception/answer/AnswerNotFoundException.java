package org.example.passpoint.global.exception.answer;

import org.example.passpoint.global.exception.BusinessException;
import org.example.passpoint.global.exception.ErrorCode;

/** 답변을 찾을 수 없을 때 */
public class AnswerNotFoundException extends BusinessException {
    public AnswerNotFoundException() {
        super(ErrorCode.ANSWER_NOT_FOUND);
    }
}
