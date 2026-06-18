package org.example.passpoint.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.example.passpoint.domain.answer.entity.AnswerStatus;
import org.example.passpoint.domain.answer.repository.AnswerRepository;
import org.example.passpoint.domain.feedback.repository.FeedbackRepository;
import org.example.passpoint.domain.studylog.service.StudyLogService;
import org.example.passpoint.domain.user.dto.UserProfileUpdateRequest;
import org.example.passpoint.domain.user.dto.UserResponse;
import org.example.passpoint.domain.user.dto.UserStatsResponse;
import org.example.passpoint.domain.user.entity.User;
import org.example.passpoint.domain.user.repository.UserRepository;
import org.example.passpoint.global.exception.user.UserNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AnswerRepository answerRepository;
    private final FeedbackRepository feedbackRepository;
    private final StudyLogService studyLogService;

    @Transactional(readOnly=true)
    public UserResponse getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        return UserResponse.from(user);
    }

    /** 프로필 수정 (닉네임, 상태 메시지) - 부분 수정: 보내지 않은 필드는 유지 */
    @Transactional
    public UserResponse updateProfile(Long userId, UserProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        user.updateProfile(request.nickname(), request.statusMessage());
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public UserStatsResponse getMyStats(Long userId) {
        long totalAnswered = answerRepository.countByUserIdAndStatusNot(userId, AnswerStatus.FAILED);
        Double averageScore = feedbackRepository.findAverageScoreByUserId(userId);
        Integer bestScore = feedbackRepository.findBestScoreByUserId(userId);

        return new UserStatsResponse(
                studyLogService.getCurrentStreak(userId),
                totalAnswered,
                averageScore != null ? (int) Math.round(averageScore) : 0,
                bestScore != null ? bestScore : 0
        );
    }
}
