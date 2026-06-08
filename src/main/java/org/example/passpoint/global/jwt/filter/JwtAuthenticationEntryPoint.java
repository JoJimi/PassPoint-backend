package org.example.passpoint.global.jwt.filter;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.passpoint.global.exception.ErrorCode;
import org.example.passpoint.global.exception.ErrorResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증되지 않은 요청이 보호된 자원에 접근할 때의 처리
 * - 토큰이 없거나 무효한 상태로 인증 필요한 API 접근 시 동작
 * - 401 상태 + 우리 ErrorResponse 형식의 JSON 응답을 내려줌
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {

        ErrorCode code = ErrorCode.UNAUTHORIZED;

        response.setStatus(code.getHttpStatus().value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse body = ErrorResponse.of(code, request.getRequestURI());
        objectMapper.writeValue(response.getWriter(), body);
    }


}
