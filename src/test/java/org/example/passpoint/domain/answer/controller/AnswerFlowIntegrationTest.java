package org.example.passpoint.domain.answer.controller;

import org.example.passpoint.TestcontainersConfiguration;
import org.example.passpoint.domain.answer.dto.request.AnswerCreateRequest;
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
 * 답변 제출 ~ 조회 흐름 통합 테스트 (Testcontainers PostgreSQL + Redis)
 * - POST /api/v1/answers → GET /api/v1/answers/{id}
 * - LLM 호출(FeedbackGenerator)만 모킹하고, DB/Redis는 실제 컨테이너로 검증한다
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AnswerFlowIntegrationTest {

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
                question.getId(), AnswerType.TEXT, "HTTPS는 SSL/TLS로 통신을 암호화합니다.");

        MvcResult submitResult = mockMvc.perform(post("/api/v1/answers")
                        .with(authentication(authOf(user.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andReturn();

        AnswerResponse submitResponse = objectMapper.readValue(
                submitResult.getResponse().getContentAsString(), AnswerResponse.class);

        mockMvc.perform(get("/api/v1/answers/{id}", submitResponse.answerId())
                        .with(authentication(authOf(user.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.answerText").value("HTTPS는 SSL/TLS로 통신을 암호화합니다."))
                .andExpect(jsonPath("$.feedback.score").value(80))
                .andExpect(jsonPath("$.feedback.coveredKeywords[0]").value("HTTPS"));

        // 답변 제출 시 StudyLogService.recordStudy()가 실제 Redis에 스트릭을 기록했는지 확인
        assertThat(redisTemplate.opsForValue().get("streak:" + user.getId())).isEqualTo("1");
    }

    @Test
    void 피드백생성실패시_FAILED상태로조회되고_feedback은null이다() throws Exception {
        given(feedbackGenerator.generate(any(), any())).willThrow(new RuntimeException("LLM 호출 실패"));

        AnswerCreateRequest request = new AnswerCreateRequest(
                question.getId(), AnswerType.TEXT, "HTTPS는 SSL/TLS로 통신을 암호화합니다.");

        MvcResult submitResult = mockMvc.perform(post("/api/v1/answers")
                        .with(authentication(authOf(user.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andReturn();

        AnswerResponse submitResponse = objectMapper.readValue(
                submitResult.getResponse().getContentAsString(), AnswerResponse.class);

        mockMvc.perform(get("/api/v1/answers/{id}", submitResponse.answerId())
                        .with(authentication(authOf(user.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.feedback").doesNotExist());
    }

    @Test
    void 답변텍스트가공백이면_400과ANSWER003에러코드를반환한다() throws Exception {
        AnswerCreateRequest request = new AnswerCreateRequest(question.getId(), AnswerType.TEXT, "   ");

        mockMvc.perform(post("/api/v1/answers")
                        .with(authentication(authOf(user.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ANSWER003"));
    }

    @Test
    void 다른사용자의답변을조회하면_403과CMN006에러코드를반환한다() throws Exception {
        given(feedbackGenerator.generate(any(), any())).willReturn(
                new FeedbackResult(80, 80, 80, 80, List.of(), List.of(), List.of()));

        User otherUser = userRepository.save(User.builder()
                .oauthProvider(OAuthProvider.GOOGLE)
                .oauthId("google-" + UUID.randomUUID())
                .email("other-" + UUID.randomUUID() + "@example.com")
                .nickname("other")
                .build());

        AnswerCreateRequest request = new AnswerCreateRequest(
                question.getId(), AnswerType.TEXT, "HTTPS는 SSL/TLS로 통신을 암호화합니다.");

        MvcResult submitResult = mockMvc.perform(post("/api/v1/answers")
                        .with(authentication(authOf(user.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        AnswerResponse submitResponse = objectMapper.readValue(
                submitResult.getResponse().getContentAsString(), AnswerResponse.class);

        mockMvc.perform(get("/api/v1/answers/{id}", submitResponse.answerId())
                        .with(authentication(authOf(otherUser.getId()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CMN006"));
    }

    private Authentication authOf(Long userId) {
        return new UsernamePasswordAuthenticationToken(userId, "test-token", Collections.emptyList());
    }
}
