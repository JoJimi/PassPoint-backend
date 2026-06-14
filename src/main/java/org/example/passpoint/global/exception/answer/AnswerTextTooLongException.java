package org.example.passpoint.global.exception.answer;

import org.example.passpoint.global.exception.BusinessException;
import org.example.passpoint.global.exception.ErrorCode;

/** 답변 텍스트가 최대 길이를 초과했을 때 */
public class AnswerTextTooLongException extends BusinessException {
    public AnswerTextTooLongException() {
        super(ErrorCode.ANSWER_TEXT_TOO_LONG);
    }
}
