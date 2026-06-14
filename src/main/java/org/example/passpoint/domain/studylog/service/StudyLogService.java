package org.example.passpoint.domain.studylog.service;

import lombok.RequiredArgsConstructor;
import org.example.passpoint.domain.studylog.dto.response.StreakResponse;
import org.example.passpoint.domain.studylog.entity.StudyLog;
import org.example.passpoint.domain.studylog.repository.StudyLogRepository;
import org.example.passpoint.domain.user.entity.User;
import org.example.passpoint.domain.user.repository.UserRepository;
import org.example.passpoint.global.exception.user.UserNotFoundException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 학습 기록(study_logs) 및 연속 학습일(streak) 관리
 * - study_logs(RDB): 일자별 풀이 수를 영속 (추이 분석용)
 * - streak(Redis): 연속 학습일/오늘 풀이 수를 캐시 (빠른 조회용) - 역할 분리
 */
@Service
@RequiredArgsConstructor
public class StudyLogService {

    private static final DateTimeFormatter SOLVED_KEY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Duration SOLVED_KEY_TTL = Duration.ofHours(48);

    private final StudyLogRepository studyLogRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;

    /** 답변 제출 성공 시 호출: study_logs 갱신 + Redis 스트릭/오늘 풀이 수 갱신 */
    @Transactional
    public void recordStudy(Long userId) {
        LocalDate today = LocalDate.now();

        studyLogRepository.findByUserIdAndStudyDate(userId, today)
                .ifPresentOrElse(
                        StudyLog::increaseSolvedCount,
                        () -> {
                            User user = userRepository.findById(userId)
                                    .orElseThrow(UserNotFoundException::new);
                            studyLogRepository.save(StudyLog.builder()
                                    .user(user)
                                    .studyDate(today)
                                    .solvedCount(1)
                                    .build());
                            updateStreak(userId, today);
                        }
                );

        String solvedKey = solvedKey(userId, today);
        redisTemplate.opsForValue().increment(solvedKey);
        redisTemplate.expire(solvedKey, SOLVED_KEY_TTL);
    }

    /** 연속 학습일 + 오늘 풀이 수 + 마지막 학습일 조회 */
    public StreakResponse getStreak(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate lastStudyDate = readLastStudyDate(userId);

        return new StreakResponse(
                resolveCurrentStreak(userId, lastStudyDate, today),
                readTodaySolved(userId, today),
                lastStudyDate
        );
    }

    /** 현재 연속 학습일 수 (통계 카드에서도 재사용) */
    public int getCurrentStreak(Long userId) {
        LocalDate today = LocalDate.now();
        return resolveCurrentStreak(userId, readLastStudyDate(userId), today);
    }

    /** 오늘 첫 풀이일 때만 호출: 어제까지 이어졌으면 +1, 아니면(끊김/최초) 1로 리셋 */
    private void updateStreak(Long userId, LocalDate today) {
        LocalDate lastStudyDate = readLastStudyDate(userId);

        int newStreak = (lastStudyDate != null && lastStudyDate.equals(today.minusDays(1)))
                ? readStreak(userId) + 1
                : 1;

        redisTemplate.opsForValue().set(streakKey(userId), String.valueOf(newStreak));
        redisTemplate.opsForValue().set(streakLastKey(userId), today.toString());
    }

    /** lastStudyDate가 오늘/어제면 저장된 스트릭 값을, 그보다 오래됐으면(끊김) 0을 반환 */
    private int resolveCurrentStreak(Long userId, LocalDate lastStudyDate, LocalDate today) {
        if (lastStudyDate == null || lastStudyDate.isBefore(today.minusDays(1))) {
            return 0;
        }
        return readStreak(userId);
    }

    private int readStreak(Long userId) {
        String value = redisTemplate.opsForValue().get(streakKey(userId));
        return value != null ? Integer.parseInt(value) : 0;
    }

    private LocalDate readLastStudyDate(Long userId) {
        String value = redisTemplate.opsForValue().get(streakLastKey(userId));
        return value != null ? LocalDate.parse(value) : null;
    }

    private int readTodaySolved(Long userId, LocalDate today) {
        String value = redisTemplate.opsForValue().get(solvedKey(userId, today));
        return value != null ? Integer.parseInt(value) : 0;
    }

    private String streakKey(Long userId) {
        return "streak:" + userId;
    }

    private String streakLastKey(Long userId) {
        return "streak:last:" + userId;
    }

    private String solvedKey(Long userId, LocalDate date) {
        return "solved:" + userId + ":" + date.format(SOLVED_KEY_DATE_FORMAT);
    }
}
