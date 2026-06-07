package org.example.passpoint.global.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 클라이언트에게 내려가는 에러 응답 포맷
 * - 코드/메시지/상태/시각/요청경로/필드별 검증오류를 담는다
 * - null 필드는 응답에서 제외(@JsonInclude NON_NULL)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String code,
        String message,
        int status,
        LocalDateTime timestamp,
        String path,
        List<FieldError> errors
) {
    /** 기본 ErrorResponse 생성 (ErrorCode 기본 메시지 사용) */
    public static ErrorResponse of(ErrorCode errorCode, String path) {
        return new ErrorResponse(
                errorCode.getCode(),
                errorCode.getMessage(),
                errorCode.getHttpStatus().value(),
                LocalDateTime.now(),
                path,
                null
        );
    }

    /** 커스텀 메시지를 포함한 ErrorResponse 생성 */
    public static ErrorResponse of(ErrorCode errorCode, String customMessage, String path) {
        return new ErrorResponse(
                errorCode.getCode(),
                customMessage,
                errorCode.getHttpStatus().value(),
                LocalDateTime.now(),
                path,
                null
        );
    }

    /** Validation 실패 시 필드 오류 목록을 포함한 ErrorResponse 생성 */
    public static ErrorResponse of(ErrorCode errorCode, List<FieldError> errors, String path) {
        return new ErrorResponse(
                errorCode.getCode(),
                errorCode.getMessage(),
                errorCode.getHttpStatus().value(),
                LocalDateTime.now(),
                path,
                errors
        );
    }

    /** 필드별 Validation 에러 정보 (어떤 필드가 왜 틀렸는지) */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FieldError(
            String field,       // 에러난 필드명
            String value,       // 입력된 잘못된 값
            String reason       // 에러 이유
    ) {
        /** Spring의 FieldError를 우리 포맷으로 변환 */
        public static FieldError of(org.springframework.validation.FieldError error) {
            return new FieldError(
                    error.getField(),
                    error.getRejectedValue() == null ? "" : error.getRejectedValue().toString(),
                    error.getDefaultMessage()
            );
        }
    }
}