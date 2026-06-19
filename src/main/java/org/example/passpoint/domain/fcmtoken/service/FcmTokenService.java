package org.example.passpoint.domain.fcmtoken.service;

import lombok.RequiredArgsConstructor;
import org.example.passpoint.domain.fcmtoken.dto.response.FcmTokenResponse;
import org.example.passpoint.domain.fcmtoken.entity.FcmToken;
import org.example.passpoint.domain.fcmtoken.repository.FcmTokenRepository;
import org.example.passpoint.domain.user.entity.User;
import org.example.passpoint.domain.user.repository.UserRepository;
import org.example.passpoint.global.exception.fcmtoken.FcmTokenNotFoundException;
import org.example.passpoint.global.exception.user.UserNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * FCM 기기 토큰 등록/삭제/조회 비즈니스 로직
 */
@Service
@RequiredArgsConstructor
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;

    /**
     * 토큰 등록 (멱등)
     * - 같은 사용자가 같은 토큰을 재등록 → updatedAt만 갱신
     * - 다른 사용자가 쓰던 토큰(기기 재로그인) → 소유자를 현재 사용자로 변경
     * - 처음 보는 토큰 → 신규 저장
     */
    @Transactional
    public FcmTokenResponse registerToken(Long userId, String token) {
        return fcmTokenRepository.findByToken(token)
                .map(existing -> {
                    if (existing.getUser().getId().equals(userId)) {
                        fcmTokenRepository.touchUpdatedAt(existing.getId());
                    } else {
                        existing.reassignOwner(findUser(userId));
                    }
                    return FcmTokenResponse.from(existing);
                })
                .orElseGet(() -> {
                    FcmToken fcmToken = FcmToken.builder()
                            .user(findUser(userId))
                            .token(token)
                            .build();
                    return FcmTokenResponse.from(fcmTokenRepository.save(fcmToken));
                });
    }

    /** 토큰 삭제 (본인 소유가 아니면 404) */
    @Transactional
    public void deleteToken(Long userId, Long tokenId) {
        FcmToken fcmToken = fcmTokenRepository.findByIdAndUserId(tokenId, userId)
                .orElseThrow(FcmTokenNotFoundException::new);
        fcmTokenRepository.delete(fcmToken);
    }

    @Transactional(readOnly = true)
    public List<String> getFcmTokensByUserId(Long userId) {
        return fcmTokenRepository.findAllByUserId(userId).stream()
                .map(FcmToken::getToken)
                .toList();
    }

    /** 발송 실패(무효/미등록 토큰) 시 자동 정리 */
    @Transactional
    public void deleteByToken(String token) {
        fcmTokenRepository.findByToken(token).ifPresent(fcmTokenRepository::delete);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
    }
}
