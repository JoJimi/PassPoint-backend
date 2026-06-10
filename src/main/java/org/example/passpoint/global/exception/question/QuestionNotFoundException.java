package org.example.passpoint.global.exception.question;

import org.example.passpoint.global.exception.BusinessException;
import org.example.passpoint.global.exception.ErrorCode;

/** 질문을 찾을 수 없을 때 */
public class QuestionNotFoundException extends BusinessException {
    public QuestionNotFoundException() {
        super(ErrorCode.QUESTION_NOT_FOUND);
    }
}
