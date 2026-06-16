package org.example.passpoint.domain.answer.controller;

import org.example.passpoint.TestcontainersConfiguration;
import org.example.passpoint.domain.answer.dto.request.AnswerCreateRequest;
import org.example.passpoint.domain.answer.dto.response.AnswerDetailResponse;
import org.example.passpoint.domain.answer.dto.response.AnswerResponse;
import org.example.passpoint.domain.answer.entity.AnswerType;
import org.example.passpoint.domain.feedback.dto.FeedbackResult;
import org.example.passpoint.domain.feedback.service.FeedbackGenerator;
import org.example.passpoint.domain.question.entity.Difficulty;
import org.example.passpoint.domain.question.entity.Question;
import org.example.passpoint.domain.question.entity.SubCategory;
import org.example.passpoint.domain.question.repository.QuestionRepository;
import org.example.passpoint.domain.user.entity.OAuthProvider;
import org.example.passpoint.domain.user.entity.User;
import org.example.passpoint.domain.user.repository.UserRepository;
import org.example.passpoint.global.s3.S3AudioStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.audio.transcription.AudioTranscription;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 답변 제출 ~ 조회 흐름 통합 테스트 (Testcontainers PostgreSQL + Redis + Kafka)
 * - POST /api/v1/answers(202, PENDING) → Kafka(feedback.requested) → FeedbackWorker → GET /api/v1/answers/{id}(DONE)
 * - LLM 호출(FeedbackGenerator)만 모킹하고, DB/Redis/Kafka는 실제 컨테이너로 검증한다
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AnswerFlowIntegrationTest {

    private static final Set<String> IN_PROGRESS_STATUSES = Set.of("PENDING", "TRANSCRIBING", "ANALYZING");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @MockitoBean
    private FeedbackGenerator feedbackGenerator;

    @MockitoBean
    private S3AudioStorageService s3AudioStorageService;

    @MockitoBean
    private OpenAiAudioTranscriptionModel transcriptionModel;

    private Question question;
    private User user;

    @BeforeEach
    void setUp() {
        question = questionRepository.save(Question.builder()
                .subCategory(SubCategory.HTTP)
                .difficulty(Difficulty.MEDIUM)
                .title("HTTP와 HTTPS의 차이는?")
                .content("HTTP와 HTTPS의 차이를 설명하시오.")
                .guidePoints(List.of("암호화", "포트 번호"))
                .keywordPool(List.of("HTTPS", "SSL/TLS", "암호화"))
                .modelAnswer("HTTPS는 SSL/TLS로 암호화된 HTTP입니다.")
                .build());

        user = userRepository.save(User.builder()
                .oauthProvider(OAuthProvider.GOOGLE)
                .oauthId("google-" + UUID.randomUUID())
                .email("tester-" + UUID.randomUUID() + "@example.com")
                .nickname("tester")
                .build());
    }

    @Test
    void 답변제출후_상세조회시_DONE상태와피드백을반환한다() throws Exception {
        FeedbackResult feedbackResult = new FeedbackResult(
                80, 80, 80, 80,
                List.of("good"), List.of("improve"), List.of("HTTPS")
        );
        given(feedbackGenerator.generate(any(), any())).willReturn(feedbackResult);

        AnswerCreateRequest request = new AnswerCreateRequest(
                question.getId(), AnswerType.TEXT, "HTTPS는 SSL/TLS로 통신을 암호화합니다.", null);

        MvcResult submitResult = mockMvc.perform(post("/api/v1/answers")
                        .with(authentication(authOf(user.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        AnswerResponse submitResponse = objectMapper.readValue(
                submitResult.getResponse().getContentAsString(), AnswerResponse.class);

        AnswerDetailResponse detail = pollUntilTerminal(submitResponse.answerId(), user.getId());

        assertThat(detail.status()).isEqualTo("DONE");
        assertThat(detail.answerText()).isEqualTo("HTTPS는 SSL/TLS로 통신을 암호화합니다.");
        assertThat(detail.feedback().score()).isEqualTo(80);
        assertThat(detail.feedback().coveredKeywords()).contains("HTTPS");

        // 답변 제출 시 StudyLogService.recordStudy()가 실제 Redis에 스트릭을 기록했는지 확인
        assertThat(redisTemplate.opsForValue().get("streak:" + user.getId())).isEqualTo("1");
    }

    @Test
    void 피드백생성실패시_FAILED상태로조회되고_feedback은null이다() throws Exception {
        given(feedbackGenerator.generate(any(), any())).willThrow(new RuntimeException("LLM 호출 실패"));

        AnswerCreateRequest request = new AnswerCreateRequest(
                question.getId(), AnswerType.TEXT, "HTTPS는 SSL/TLS로 통신을 암호화합니다.", null);

        MvcResult submitResult = mockMvc.perform(post("/api/v1/answers")
                        .with(authentication(authOf(user.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        AnswerResponse submitResponse = objectMapper.readValue(
                submitResult.getResponse().getContentAsString(), AnswerResponse.class);

        AnswerDetailResponse detail = pollUntilTerminal(submitResponse.answerId(), user.getId());

        assertThat(detail.status()).isEqualTo("FAILED");
        assertThat(detail.feedback()).isNull();
    }

    @Test
    void 답변텍스트가공백이면_400과ANSWER003에러코드를반환한다() throws Exception {
        AnswerCreateRequest request = new AnswerCreateRequest(question.getId(), AnswerType.TEXT, "   ", null);

        mockMvc.perform(post("/api/v1/answers")
                        .with(authentication(authOf(user.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ANSWER003"));
    }

    @Test
    void 다른사용자의답변을조회하면_403과CMN006에러코드를반환한다() throws Exception {
        User otherUser = userRepository.save(User.builder()
                .oauthProvider(OAuthProvider.GOOGLE)
                .oauthId("google-" + UUID.randomUUID())
                .email("other-" + UUID.randomUUID() + "@example.com")
                .nickname("other")
                .build());

        AnswerCreateRequest request = new AnswerCreateRequest(
                question.getId(), AnswerType.TEXT, "HTTPS는 SSL/TLS로 통신을 암호화합니다.", null);

        MvcResult submitResult = mockMvc.perform(post("/api/v1/answers")
                        .with(authentication(authOf(user.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andReturn();

        AnswerResponse submitResponse = objectMapper.readValue(
                submitResult.getResponse().getContentAsString(), AnswerResponse.class);

        mockMvc.perform(get("/api/v1/answers/{id}", submitResponse.answerId())
                        .with(authentication(authOf(otherUser.getId()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CMN006"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void 음성답변제출후_STT파이프라인거쳐_DONE상태와피드백반환한다() throws Exception {
        String fakeAudioKey = "answers/audio/test.m4a";
        String sttText = "HTTP는 암호화 없이 데이터를 전송하는 프로토콜입니다.";
        String presignedUrl = "http://minio/presigned/audio.m4a";

        // S3 mock: 다운로드 + presigned 재생 URL
        given(s3AudioStorageService.downloadAudio(fakeAudioKey)).willReturn(new byte[]{1, 2, 3});
        given(s3AudioStorageService.generateDownloadPresignedUrl(fakeAudioKey)).willReturn(presignedUrl);

        // STT mock: response.getResult().getOutput() → sttText
        AudioTranscription mockAudioTranscription = Mockito.mock(AudioTranscription.class);
        given(mockAudioTranscription.getOutput()).willReturn(sttText);
        AudioTranscriptionResponse mockTranscriptionResponse = Mockito.mock(AudioTranscriptionResponse.class);
        given(mockTranscriptionResponse.getResult()).willReturn(mockAudioTranscription);
        given(transcriptionModel.call(any(AudioTranscriptionPrompt.class))).willReturn(mockTranscriptionResponse);

        // LLM mock
        FeedbackResult feedbackResult = new FeedbackResult(
                75, 75, 75, 75,
                List.of("좋은 설명"), List.of("HTTPS 추가 언급"), List.of("HTTPS"));
        given(feedbackGenerator.generate(any(), any())).willReturn(feedbackResult);

        // 음성 답변 제출 → 202 PENDING
        AnswerCreateRequest request = new AnswerCreateRequest(
                question.getId(), AnswerType.VOICE, null, fakeAudioKey);

        MvcResult submitResult = mockMvc.perform(post("/api/v1/answers")
                        .with(authentication(authOf(user.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.type").value("VOICE"))
                .andReturn();

        AnswerResponse submitResponse = objectMapper.readValue(
                submitResult.getResponse().getContentAsString(), AnswerResponse.class);

        // PENDING → TRANSCRIBING → ANALYZING → DONE 폴링
        AnswerDetailResponse detail = pollUntilTerminal(submitResponse.answerId(), user.getId());

        assertThat(detail.status()).isEqualTo("DONE");
        assertThat(detail.answerText()).isEqualTo(sttText);       // STT 결과가 채워져야 함
        assertThat(detail.audioUrl()).isEqualTo(presignedUrl);    // presigned 재생 URL
        assertThat(detail.feedback().score()).isEqualTo(75);
        assertThat(detail.feedback().coveredKeywords()).contains("HTTPS");
    }

    @Test
    @SuppressWarnings("unchecked")
    void 음성답변STT실패시_FAILED상태로조회되고_feedback은null이다() throws Exception {
        String fakeAudioKey = "answers/audio/fail-test.m4a";

        // S3 다운로드는 성공하지만 STT 호출에서 예외
        given(s3AudioStorageService.downloadAudio(fakeAudioKey)).willReturn(new byte[]{1, 2, 3});
        given(transcriptionModel.call(any(AudioTranscriptionPrompt.class)))
                .willThrow(new RuntimeException("OpenAI API 장애"));

        AnswerCreateRequest request = new AnswerCreateRequest(
                question.getId(), AnswerType.VOICE, null, fakeAudioKey);

        MvcResult submitResult = mockMvc.perform(post("/api/v1/answers")
                        .with(authentication(authOf(user.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andReturn();

        AnswerResponse submitResponse = objectMapper.readValue(
                submitResult.getResponse().getContentAsString(), AnswerResponse.class);

        AnswerDetailResponse detail = pollUntilTerminal(submitResponse.answerId(), user.getId());

        assertThat(detail.status()).isEqualTo("FAILED");
        assertThat(detail.feedback()).isNull();
    }

    private Authentication authOf(Long userId) {
        return new UsernamePasswordAuthenticationToken(userId, "test-token", Collections.emptyList());
    }

    /** Kafka(feedback.requested -> FeedbackWorker) 처리가 끝나 DONE/FAILED가 될 때까지 폴링한다 */
    private AnswerDetailResponse pollUntilTerminal(Long answerId, Long userId) throws Exception {
        long deadline = System.currentTimeMillis() + 10_000;

        while (true) {
            MvcResult result = mockMvc.perform(get("/api/v1/answers/{id}", answerId)
                            .with(authentication(authOf(userId))))
                    .andReturn();
            AnswerDetailResponse detail = objectMapper.readValue(
                    result.getResponse().getContentAsString(), AnswerDetailResponse.class);

            if (!IN_PROGRESS_STATUSES.contains(detail.status())) {
                return detail;
            }
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("답변 처리가 제한 시간 내에 끝나지 않음: status=" + detail.status());
            }
            Thread.sleep(200);
        }
    }
}
