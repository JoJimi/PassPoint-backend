package org.example.passpoint.global.exception.answer;

import org.example.passpoint.global.exception.BusinessException;
import org.example.passpoint.global.exception.ErrorCode;

/** 본인의 답변이 아닌 리소스에 접근할 때 */
public class AnswerAccessDeniedException extends BusinessException {
    public AnswerAccessDeniedException() {
        super(ErrorCode.ACCESS_DENIED, "본인의 답변만 조회할 수 있습니다.");
    }
}
