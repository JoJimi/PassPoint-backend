package org.example.passpoint.domain.answer.service;

import org.example.passpoint.domain.answer.dto.request.AnswerCreateRequest;
import org.example.passpoint.domain.answer.dto.response.AnswerResponse;
import org.example.passpoint.domain.answer.entity.Answer;
import org.example.passpoint.domain.answer.entity.AnswerStatus;
import org.example.passpoint.domain.answer.entity.AnswerType;
import org.example.passpoint.domain.answer.repository.AnswerRepository;
import org.example.passpoint.domain.feedback.dto.FeedbackResult;
import org.example.passpoint.domain.feedback.repository.FeedbackRepository;
import org.example.passpoint.domain.feedback.service.FeedbackGenerator;
import org.example.passpoint.domain.question.entity.Difficulty;
import org.example.passpoint.domain.question.entity.Question;
import org.example.passpoint.domain.question.entity.SubCategory;
import org.example.passpoint.domain.question.repository.QuestionRepository;
import org.example.passpoint.domain.studylog.service.StudyLogService;
import org.example.passpoint.domain.user.entity.OAuthProvider;
import org.example.passpoint.domain.user.entity.User;
import org.example.passpoint.domain.user.repository.UserRepository;
import org.example.passpoint.global.exception.answer.AnswerTextRequiredException;
import org.example.passpoint.global.exception.answer.AnswerTextTooLongException;
import org.example.passpoint.global.exception.answer.VoiceNotSupportedException;
import org.example.passpoint.global.exception.question.QuestionNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * AnswerService.submit()의 tx 분리 흐름 및 입력 검증 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class AnswerServiceTest {

    @Mock
    private AnswerRepository answerRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FeedbackRepository feedbackRepository;
    @Mock
    private FeedbackGenerator feedbackGenerator;
    @Mock
    private AnswerWriteService answerWriteService;
    @Mock
    private StudyLogService studyLogService;

    @InjectMocks
    private AnswerService answerService;

    private static final Long USER_ID = 1L;
    private static final Long QUESTION_ID = 1L;
    private static final Long ANSWER_ID = 100L;

    private Question question;
    private User user;
    private Answer answer;

    @BeforeEach
    void setUp() {
        question = Question.builder()
                .subCategory(SubCategory.HTTP)
                .difficulty(Difficulty.MEDIUM)
                .title("HTTP와 HTTPS의 차이는?")
                .content("HTTP와 HTTPS의 차이를 설명하시오.")
                .hint(null)
                .guidePoints(List.of("암호화", "포트 번호"))
                .keywordPool(List.of("HTTPS", "SSL/TLS", "암호화"))
                .modelAnswer("HTTPS는 SSL/TLS로 암호화된 HTTP입니다.")
                .build();
        ReflectionTestUtils.setField(question, "id", QUESTION_ID);

        user = User.builder()
                .oauthProvider(OAuthProvider.GOOGLE)
                .oauthId("google-1")
                .email("tester@example.com")
                .nickname("tester")
                .build();
        ReflectionTestUtils.setField(user, "id", USER_ID);

        answer = Answer.builder()
                .user(user)
                .question(question)
                .type(AnswerType.TEXT)
                .answerText("HTTPS는 SSL/TLS로 통신을 암호화합니다.")
                .status(AnswerStatus.ANALYZING)
                .build();
        ReflectionTestUtils.setField(answer, "id", ANSWER_ID);
    }

    @Test
    void 답변제출_성공시_DONE상태와피드백저장() {
        AnswerCreateRequest request = new AnswerCreateRequest(QUESTION_ID, AnswerType.TEXT, "HTTPS는 SSL/TLS로 통신을 암호화합니다.");
        FeedbackResult feedbackResult = new FeedbackResult(80, 80, 80, 80, List.of("good"), List.of("improve"), List.of("HTTPS"));

        given(questionRepository.findById(QUESTION_ID)).willReturn(Optional.of(question));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(answerWriteService.createAnswer(user, question, AnswerType.TEXT, request.answerText())).willReturn(answer);
        given(feedbackGenerator.generate(question, request.answerText())).willReturn(feedbackResult);

        AnswerResponse response = answerService.submit(USER_ID, request);

        assertThat(response.status()).isEqualTo("DONE");
        assertThat(response.answerId()).isEqualTo(ANSWER_ID);
        verify(answerWriteService).completeFeedback(ANSWER_ID, feedbackResult);
        verify(answerWriteService, never()).markFailed(any());
        verify(studyLogService).recordStudy(USER_ID);
    }

    @Test
    void 답변제출_피드백생성실패시_FAILED상태와실패마킹() {
        AnswerCreateRequest request = new AnswerCreateRequest(QUESTION_ID, AnswerType.TEXT, "HTTPS는 SSL/TLS로 통신을 암호화합니다.");

        given(questionRepository.findById(QUESTION_ID)).willReturn(Optional.of(question));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(answerWriteService.createAnswer(user, question, AnswerType.TEXT, request.answerText())).willReturn(answer);
        given(feedbackGenerator.generate(question, request.answerText())).willThrow(new RuntimeException("LLM 호출 실패"));

        AnswerResponse response = answerService.submit(USER_ID, request);

        assertThat(response.status()).isEqualTo("FAILED");
        verify(answerWriteService).markFailed(ANSWER_ID);
        verify(answerWriteService, never()).completeFeedback(any(), any());
    }

    @Test
    void 답변제출_study_log기록은_LLM호출보다먼저수행된다() {
        AnswerCreateRequest request = new AnswerCreateRequest(QUESTION_ID, AnswerType.TEXT, "HTTPS는 SSL/TLS로 통신을 암호화합니다.");
        FeedbackResult feedbackResult = new FeedbackResult(80, 80, 80, 80, List.of(), List.of(), List.of());

        given(questionRepository.findById(QUESTION_ID)).willReturn(Optional.of(question));
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(answerWriteService.createAnswer(user, question, AnswerType.TEXT, request.answerText())).willReturn(answer);
        given(feedbackGenerator.generate(question, request.answerText())).willReturn(feedbackResult);

        answerService.submit(USER_ID, request);

        InOrder inOrder = inOrder(answerWriteService, studyLogService, feedbackGenerator);
        inOrder.verify(answerWriteService).createAnswer(user, question, AnswerType.TEXT, request.answerText());
        inOrder.verify(studyLogService).recordStudy(USER_ID);
        inOrder.verify(feedbackGenerator).generate(question, request.answerText());
    }

    @Test
    void 답변제출_질문이존재하지않으면_QuestionNotFoundException() {
        AnswerCreateRequest request = new AnswerCreateRequest(QUESTION_ID, AnswerType.TEXT, "정상적인 답변 텍스트입니다.");
        given(questionRepository.findById(QUESTION_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> answerService.submit(USER_ID, request))
                .isInstanceOf(QuestionNotFoundException.class);

        verifyNoInteractions(answerWriteService, studyLogService);
    }

    @Test
    void 답변제출_타입이VOICE면_VoiceNotSupportedException() {
        AnswerCreateRequest request = new AnswerCreateRequest(QUESTION_ID, AnswerType.VOICE, "음성 답변");

        assertThatThrownBy(() -> answerService.submit(USER_ID, request))
                .isInstanceOf(VoiceNotSupportedException.class);

        verifyNoInteractions(questionRepository, userRepository, answerWriteService, studyLogService);
    }

    @Test
    void 답변제출_답변텍스트가공백이면_AnswerTextRequiredException() {
        AnswerCreateRequest request = new AnswerCreateRequest(QUESTION_ID, AnswerType.TEXT, "   ");

        assertThatThrownBy(() -> answerService.submit(USER_ID, request))
                .isInstanceOf(AnswerTextRequiredException.class);

        verifyNoInteractions(questionRepository, userRepository, answerWriteService, studyLogService);
    }

    @Test
    void 답변제출_답변텍스트가최대길이초과면_AnswerTextTooLongException() {
        String tooLong = "a".repeat(3001);
        AnswerCreateRequest request = new AnswerCreateRequest(QUESTION_ID, AnswerType.TEXT, tooLong);

        assertThatThrownBy(() -> answerService.submit(USER_ID, request))
                .isInstanceOf(AnswerTextTooLongException.class);

        verifyNoInteractions(questionRepository, userRepository, answerWriteService, studyLogService);
    }
}
