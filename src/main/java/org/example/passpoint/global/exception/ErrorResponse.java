package org.example.passpoint.global.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String code,
        String message,
        int status,
        LocalDateTime timestamp,
        String path,
        List<FieldError> errors
) {
    /**
     * 기본 ErrorResponse 생성
     */
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

    /**
     * 커스텀 메시지를 포함한 ErrorResponse 생성
     */
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

    /**
     * Validation 에러를 포함한 ErrorResponse 생성
     */
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

    /**
     * 필드별 Validation 에러 정보
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FieldError(
            String field,       // 에러난 필드명
            String value,       // 입력된 잘못된 값
            String reason       // 에러 이유
    ) {
        /**
         * Spring FieldError로부터 생성
         */
        public static FieldError of(org.springframework.validation.FieldError error) {
            return new FieldError(
                    error.getField(),
                    error.getRejectedValue() == null ? "" : error.getRejectedValue().toString(),
                    error.getDefaultMessage()
            );
        }
    }
}
