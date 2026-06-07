package org.example.passpoint.global.exception;

import lombok.Getter;

/**
 * 비즈니스 로직에서 발생하는 예외의 최상위 클래스
 * - 모든 도메인 예외(UserNotFoundException 등)는 이 클래스를 상속한다
 * - ErrorCode를 담아 GlobalExceptionHandler가 일관된 응답으로 변환한다
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /** 기본 생성자 - ErrorCode의 기본 메시지를 사용 */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /** 커스텀 메시지를 직접 지정하는 생성자 */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /** 원인 예외(cause)를 함께 담는 생성자 - 예외 추적용 */
    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}