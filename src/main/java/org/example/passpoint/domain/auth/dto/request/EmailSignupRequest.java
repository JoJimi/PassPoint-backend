package org.example.passpoint.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 이메일 회원가입 요청
 */
public record EmailSignupRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 64, message = "비밀번호는 8~64자로 입력해주세요.")
        String password,

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 1, max = 20, message = "닉네임은 1~20자로 입력해주세요.")
        String nickname
) {
}
