package org.example.passpoint.global.exception.answer;

import org.example.passpoint.global.exception.BusinessException;
import org.example.passpoint.global.exception.ErrorCode;

/** 답변 텍스트가 비어있을 때 */
public class AnswerTextRequiredException extends BusinessException {
    public AnswerTextRequiredException() {
        super(ErrorCode.ANSWER_TEXT_REQUIRED);
    }
}
