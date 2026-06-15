package org.example.passpoint.domain.studylog.service;

import org.example.passpoint.domain.studylog.entity.StudyLog;
import org.example.passpoint.domain.studylog.repository.StudyLogRepository;
import org.example.passpoint.domain.user.entity.OAuthProvider;
import org.example.passpoint.domain.user.entity.User;
import org.example.passpoint.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * StudyLogService.recordStudy()의 스트릭(streak) 로직 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class StudyLogServiceTest {

    @Mock
    private StudyLogRepository studyLogRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private StudyLogService studyLogService;

    private static final Long USER_ID = 1L;
    private static final DateTimeFormatter SOLVED_KEY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private LocalDate today;
    private User user;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        user = User.builder()
                .oauthProvider(OAuthProvider.GOOGLE)
                .oauthId("google-1")
                .email("tester@example.com")
                .nickname("tester")
                .build();
        ReflectionTestUtils.setField(user, "id", USER_ID);
    }

    @Test
    void 어제학습기록이있으면_오늘첫풀이시_스트릭이1증가한다() {
        given(studyLogRepository.findByUserIdAndStudyDate(USER_ID, today)).willReturn(Optional.empty());
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(valueOperations.get("streak:last:" + USER_ID)).willReturn(today.minusDays(1).toString());
        given(valueOperations.get("streak:" + USER_ID)).willReturn("3");

        studyLogService.recordStudy(USER_ID);

        verify(valueOperations).set("streak:" + USER_ID, "4");
        verify(valueOperations).set("streak:last:" + USER_ID, today.toString());
        verify(studyLogRepository).save(any(StudyLog.class));
    }

    @Test
    void 이틀전학습기록이있으면_오늘첫풀이시_스트릭이1로리셋된다() {
        given(studyLogRepository.findByUserIdAndStudyDate(USER_ID, today)).willReturn(Optional.empty());
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(valueOperations.get("streak:last:" + USER_ID)).willReturn(today.minusDays(2).toString());

        studyLogService.recordStudy(USER_ID);

        verify(valueOperations).set("streak:" + USER_ID, "1");
        verify(valueOperations).set("streak:last:" + USER_ID, today.toString());
    }

    @Test
    void 오늘이미풀었으면_재풀이시_스트릭이유지된다() {
        StudyLog existingLog = mock(StudyLog.class);
        given(studyLogRepository.findByUserIdAndStudyDate(USER_ID, today)).willReturn(Optional.of(existingLog));

        studyLogService.recordStudy(USER_ID);

        verify(existingLog).increaseSolvedCount();
        verify(studyLogRepository, never()).save(any());
        verifyNoInteractions(userRepository);
        verify(valueOperations, never()).set(eq("streak:" + USER_ID), any());
        verify(valueOperations, never()).set(eq("streak:last:" + USER_ID), any());
    }

    @Test
    void recordStudy호출시_오늘풀이수redis카운터가증가하고TTL이설정된다() {
        given(studyLogRepository.findByUserIdAndStudyDate(USER_ID, today)).willReturn(Optional.empty());
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(valueOperations.get("streak:last:" + USER_ID)).willReturn(null);

        studyLogService.recordStudy(USER_ID);

        String solvedKey = "solved:" + USER_ID + ":" + today.format(SOLVED_KEY_DATE_FORMAT);
        verify(valueOperations).increment(solvedKey);
        verify(redisTemplate).expire(solvedKey, Duration.ofHours(48));
    }
}
