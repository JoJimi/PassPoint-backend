package org.example.passpoint.domain.user.dto;

import jakarta.validation.constraints.Size;

/**
 * 프로필 수정 요청
 * - 부분 수정: 필드를 보내지 않으면(null) 해당 값은 유지된다 (User.updateProfile 참고)
 */
public record UserProfileUpdateRequest(
        @Size(min = 1, max = 20, message = "닉네임은 1~20자로 입력해주세요.")
        String nickname,

        @Size(max = 50, message = "상태 메시지는 50자 이하로 입력해주세요.")
        String statusMessage
) {
}
