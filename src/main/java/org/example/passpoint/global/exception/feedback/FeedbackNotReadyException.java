package org.example.passpoint.global.exception.feedback;

import org.example.passpoint.global.exception.BusinessException;
import org.example.passpoint.global.exception.ErrorCode;

/** 답변 처리가 끝나지 않아(status != DONE) 피드백이 아직 없을 때 */
public class FeedbackNotReadyException extends BusinessException {
    public FeedbackNotReadyException() {
        super(ErrorCode.FEEDBACK_NOT_READY);
    }
}
