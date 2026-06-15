package org.example.passpoint.domain.feedback.worker;

import org.example.passpoint.domain.answer.entity.Answer;
import org.example.passpoint.domain.answer.entity.AnswerStatus;
import org.example.passpoint.domain.answer.entity.AnswerType;
import org.example.passpoint.domain.answer.service.AnswerWriteService;
import org.example.passpoint.domain.feedback.dto.FeedbackResult;
import org.example.passpoint.domain.feedback.service.FeedbackGenerator;
import org.example.passpoint.domain.question.entity.Difficulty;
import org.example.passpoint.domain.question.entity.Question;
import org.example.passpoint.domain.question.entity.SubCategory;
import org.example.passpoint.domain.user.entity.OAuthProvider;
import org.example.passpoint.domain.user.entity.User;
import org.example.passpoint.global.kafka.event.FeedbackRequestedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * FeedbackWorker(feedback.requested 컨슈머)의 멱등성 및 성공/실패 분기 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class FeedbackWorkerTest {

    @Mock
    private AnswerWriteService answerWriteService;
    @Mock
    private FeedbackGenerator feedbackGenerator;

    @InjectMocks
    private FeedbackWorker feedbackWorker;

    private static final Long ANSWER_ID = 100L;

    private Answer answer;

    @BeforeEach
    void setUp() {
        Question question = Question.builder()
                .subCategory(SubCategory.HTTP)
                .difficulty(Difficulty.MEDIUM)
                .title("HTTP와 HTTPS의 차이는?")
                .content("HTTP와 HTTPS의 차이를 설명하시오.")
                .guidePoints(List.of("암호화", "포트 번호"))
                .keywordPool(List.of("HTTPS", "SSL/TLS", "암호화"))
                .modelAnswer("HTTPS는 SSL/TLS로 암호화된 HTTP입니다.")
                .build();
        ReflectionTestUtils.setField(question, "id", 1L);

        User user = User.builder()
                .oauthProvider(OAuthProvider.GOOGLE)
                .oauthId("google-1")
                .email("tester@example.com")
                .nickname("tester")
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

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
    void 정상처리시_피드백생성후_완료처리한다() {
        FeedbackResult result = new FeedbackResult(80, 80, 80, 80, List.of("good"), List.of("improve"), List.of("HTTPS"));

        given(answerWriteService.markAnalyzing(ANSWER_ID)).willReturn(answer);
        given(feedbackGenerator.generate(answer.getQuestion(), answer.getAnswerText())).willReturn(result);

        feedbackWorker.onFeedbackRequested(new FeedbackRequestedEvent(ANSWER_ID));

        verify(answerWriteService).completeFeedback(ANSWER_ID, result);
        verify(answerWriteService, never()).markFailed(any());
    }

    @Test
    void 이미처리된답변이면_멱등하게건너뛴다() {
        given(answerWriteService.markAnalyzing(ANSWER_ID)).willReturn(null);

        feedbackWorker.onFeedbackRequested(new FeedbackRequestedEvent(ANSWER_ID));

        verifyNoInteractions(feedbackGenerator);
        verify(answerWriteService, never()).completeFeedback(any(), any());
        verify(answerWriteService, never()).markFailed(any());
    }

    @Test
    void LLM호출실패시_FAILED로마킹한다() {
        given(answerWriteService.markAnalyzing(ANSWER_ID)).willReturn(answer);
        given(feedbackGenerator.generate(answer.getQuestion(), answer.getAnswerText()))
                .willThrow(new RuntimeException("LLM 호출 실패"));

        feedbackWorker.onFeedbackRequested(new FeedbackRequestedEvent(ANSWER_ID));

        verify(answerWriteService).markFailed(ANSWER_ID);
        verify(answerWriteService, never()).completeFeedback(any(), any());
    }
}
