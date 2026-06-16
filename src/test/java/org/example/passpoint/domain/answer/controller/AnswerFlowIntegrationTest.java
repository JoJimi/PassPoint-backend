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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
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
 * 답변 제출 ~ 조회 흐름 통합 테스트 (Testcontainers PostgreSQL + Redis + Kafka + MinIO)
 * - DB·Redis·Kafka·S3(MinIO)는 실제 singleton 컨테이너로 검증한다
 * - 유료 외부 API(LLM·STT)만 mock: 비용·비결정성 차단
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AnswerFlowIntegrationTest {

    private static final Set<String> IN_PROGRESS_STATUSES = Set.of("PENDING", "TRANSCRIBING", "ANALYZING");

    /** MinIO 컨테이너 속성을 Spring 컨텍스트에 주입 (TestcontainersConfiguration 참조로 static initializer 트리거) */
    @DynamicPropertySource
    static void minioProperties(DynamicPropertyRegistry registry) {
        registry.add("cloud.aws.s3.endpoint", TestcontainersConfiguration::minioEndpoint);
        registry.add("cloud.aws.s3.access-key", () -> TestcontainersConfiguration.MINIO_USER);
        registry.add("cloud.aws.s3.secret-key", () -> TestcontainersConfiguration.MINIO_PASSWORD);
        registry.add("cloud.aws.s3.bucket", () -> TestcontainersConfiguration.MINIO_BUCKET);
        registry.add("cloud.aws.s3.region", () -> "us-east-1");
        registry.add("cloud.aws.s3.path-style", () -> "true");
        registry.add("cloud.aws.s3.presigned-url-expiry-minutes", () -> "5");
    }

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

    @Autowired
    private S3Client s3Client;

    @MockitoBean
    private FeedbackGenerator feedbackGenerator;

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

    // ─── 텍스트 답변 흐름 ──────────────────────────────────────────────────────

    @Test
    void 텍스트_답변제출후_상세조회시_DONE상태와피드백을반환한다() throws Exception {
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

        // StudyLogService.recordStudy()가 실제 Redis에 스트릭을 기록했는지 확인
        assertThat(redisTemplate.opsForValue().get("streak:" + user.getId())).isEqualTo("1");
    }

    @Test
    void 텍스트_피드백생성실패시_FAILED상태로조회되고_feedback은null이다() throws Exception {
        given(feedbackGenerator.generate(any(), any())).willThrow(new RuntimeException("LLM 호출 실패"));

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

    // ─── 음성 답변 흐름 (MinIO 실제 연동 + STT mock) ─────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void 음성_STT성공시_PENDING에서DONE까지_answerText와audioUrl이채워진다() throws Exception {
        String audioKey = "answers/audio/" + UUID.randomUUID() + ".m4a";
        String sttText = "HTTP는 암호화 없이 데이터를 전송하는 프로토콜입니다.";

        // 실제 MinIO에 테스트 음성 파일 업로드 (SttWorker가 다운로드할 파일)
        uploadToMinio(audioKey, new byte[]{0, 1, 2, 3});

        // STT mock: OpenAI 호출 없이 텍스트 반환
        AudioTranscription mockTranscription = Mockito.mock(AudioTranscription.class);
        given(mockTranscription.getOutput()).willReturn(sttText);
        AudioTranscriptionResponse mockResponse = Mockito.mock(AudioTranscriptionResponse.class);
        given(mockResponse.getResult()).willReturn(mockTranscription);
        given(transcriptionModel.call(any(AudioTranscriptionPrompt.class))).willReturn(mockResponse);

        // LLM mock
        given(feedbackGenerator.generate(any(), any())).willReturn(
                new FeedbackResult(75, 75, 75, 75,
                        List.of("좋은 설명"), List.of("HTTPS 추가 언급"), List.of("HTTPS")));

        // 음성 답변 제출 → 202 PENDING
        AnswerCreateRequest request = new AnswerCreateRequest(
                question.getId(), AnswerType.VOICE, null, audioKey);

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
        assertThat(detail.answerText()).isEqualTo(sttText);   // STT 결과 반영
        assertThat(detail.audioUrl()).isNotNull();             // MinIO presigned GET URL
        assertThat(detail.audioUrl()).contains(audioKey);     // URL에 S3 키 포함
        assertThat(detail.feedback().score()).isEqualTo(75);
        assertThat(detail.feedback().coveredKeywords()).contains("HTTPS");
    }

    @Test
    @SuppressWarnings("unchecked")
    void 음성_STT실패시_FAILED상태로조회되고_feedback은null이다() throws Exception {
        String audioKey = "answers/audio/" + UUID.randomUUID() + "-fail.m4a";

        // 실제 MinIO에 파일 업로드 (다운로드는 성공, STT만 실패)
        uploadToMinio(audioKey, new byte[]{0, 1, 2, 3});

        given(transcriptionModel.call(any(AudioTranscriptionPrompt.class)))
                .willThrow(new RuntimeException("OpenAI API 장애"));

        AnswerCreateRequest request = new AnswerCreateRequest(
                question.getId(), AnswerType.VOICE, null, audioKey);

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

    // ─── helpers ──────────────────────────────────────────────────────────────

    private void uploadToMinio(String key, byte[] content) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(TestcontainersConfiguration.MINIO_BUCKET)
                        .key(key)
                        .build(),
                RequestBody.fromBytes(content)
        );
    }

    private Authentication authOf(Long userId) {
        return new UsernamePasswordAuthenticationToken(userId, "test-token", Collections.emptyList());
    }

    /** DONE/FAILED 가 될 때까지 GET /answers/{id} 를 폴링한다 (최대 10초) */
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
