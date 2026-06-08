package org.example.passpoint.domain.user.dto;

import org.example.passpoint.domain.user.entity.User;

/**
 * 사용자 프로필 응답
 */
public record UserResponse(
        Long id,
        String email,
        String nickname,
        String statusMessage) {

    /** User 엔티티 → 응답 DTO 변환 */
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getStatusMessage()
        );
    }
}
