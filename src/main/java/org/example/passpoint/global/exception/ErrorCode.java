package org.example.passpoint.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 애플리케이션 전역에서 사용하는 에러 코드 정의
 * - HTTP 상태, 고유 코드(식별자), 사용자 메시지를 한 곳에서 관리
 * - 새 예외가 필요해질 때 항목을 추가한다 (사용하는 것만 정의)
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 공통 (CMN)
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "CMN001", "서버 오류가 발생했습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "CMN002", "잘못된 입력 값입니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "CMN003", "잘못된 타입입니다."),
    MISSING_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, "CMN004", "필수 파라미터가 누락되었습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "CMN005", "지원하지 않는 HTTP 메서드입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "CMN006", "접근이 거부되었습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "CMN007", "요청한 리소스를 찾을 수 없습니다."),
    EXTERNAL_SERVICE_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "CMN009", "외부 서비스가 일시적으로 사용 불가능합니다."),

    // 인증/인가 (AUTH)
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH001", "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH002", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH003", "만료된 토큰입니다."),
    REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "AUTH004", "리프레시 토큰이 일치하지 않습니다."),

    // OAuth
    OAUTH_AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "AUTH101", "OAuth 인증에 실패했습니다."),

    // 사용자 (USER)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER001", "사용자를 찾을 수 없습니다."),

    // 질문 (QUESTION)
    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "QUESTION001", "질문을 찾을 수 없습니다."),

    // 답변 (ANSWER)
    ANSWER_NOT_FOUND(HttpStatus.NOT_FOUND, "ANSWER001", "답변을 찾을 수 없습니다."),
    VOICE_NOT_SUPPORTED_YET(HttpStatus.BAD_REQUEST, "ANSWER002", "음성 답변은 아직 지원하지 않습니다."),
    ANSWER_TEXT_REQUIRED(HttpStatus.BAD_REQUEST, "ANSWER003", "답변 내용을 입력해주세요."),
    ANSWER_TEXT_TOO_LONG(HttpStatus.BAD_REQUEST, "ANSWER004", "답변 내용이 너무 길어요. (최대 3000자)"),
    AUDIO_KEY_REQUIRED(HttpStatus.BAD_REQUEST, "ANSWER005", "음성 답변의 오디오 키를 입력해주세요."),

    // 피드백 (FEEDBACK)
    FEEDBACK_NOT_READY(HttpStatus.NOT_FOUND, "FEEDBACK001", "아직 피드백이 생성되지 않았습니다."),

    // 즐겨찾기 (BOOKMARK)
    BOOKMARK_NOT_FOUND(HttpStatus.NOT_FOUND, "BOOKMARK001", "즐겨찾기를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;   // 응답 HTTP 상태 코드
    private final String code;             // 에러 식별 코드 (클라이언트 분기용)
    private final String message;          // 기본 사용자 메시지
}