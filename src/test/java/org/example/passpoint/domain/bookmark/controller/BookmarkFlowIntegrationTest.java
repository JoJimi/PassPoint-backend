package org.example.passpoint.domain.bookmark.controller;

import org.example.passpoint.TestcontainersConfiguration;
import org.example.passpoint.domain.bookmark.dto.request.BookmarkCreateRequest;
import org.example.passpoint.domain.bookmark.dto.response.BookmarkResponse;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 즐겨찾기 등록~삭제 흐름 통합 테스트 (Testcontainers PostgreSQL)
 * - POST /api/v1/bookmarks → GET /api/v1/bookmarks → DELETE /api/v1/bookmarks/{questionId}
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class BookmarkFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private UserRepository userRepository;

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
    void 즐겨찾기_추가후_목록에서_조회된다() throws Exception {
        BookmarkCreateRequest request = new BookmarkCreateRequest(question.getId());

        mockMvc.perform(post("/api/v1/bookmarks")
                        .with(authentication(authOf(user.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId").value(question.getId()));

        mockMvc.perform(get("/api/v1/bookmarks")
                        .with(authentication(authOf(user.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].questionId").value(question.getId()))
                .andExpect(jsonPath("$.content[0].title").value(question.getTitle()))
                .andExpect(jsonPath("$.content[0].mainCategory").value(question.getMainCategory().name()));
    }

    @Test
    void 이미등록된_질문을_다시추가하면_기존항목을그대로반환한다() throws Exception {
        BookmarkCreateRequest request = new BookmarkCreateRequest(question.getId());

        MvcResult firstResult = mockMvc.perform(post("/api/v1/bookmarks")
                        .with(authentication(authOf(user.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        BookmarkResponse firstResponse = objectMapper.readValue(
                firstResult.getResponse().getContentAsString(), BookmarkResponse.class);

        MvcResult secondResult = mockMvc.perform(post("/api/v1/bookmarks")
                        .with(authentication(authOf(user.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        BookmarkResponse secondResponse = objectMapper.readValue(
                secondResult.getResponse().getContentAsString(), BookmarkResponse.class);

        org.assertj.core.api.Assertions.assertThat(secondResponse.bookmarkId()).isEqualTo(firstResponse.bookmarkId());

        mockMvc.perform(get("/api/v1/bookmarks")
                        .with(authentication(authOf(user.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void 즐겨찾기_삭제후_목록에서_사라진다() throws Exception {
        BookmarkCreateRequest request = new BookmarkCreateRequest(question.getId());

        mockMvc.perform(post("/api/v1/bookmarks")
                        .with(authentication(authOf(user.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/bookmarks/{questionId}", question.getId())
                        .with(authentication(authOf(user.getId()))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/bookmarks")
                        .with(authentication(authOf(user.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void 등록되지않은_즐겨찾기를_삭제하면_404와BOOKMARK001을반환한다() throws Exception {
        mockMvc.perform(delete("/api/v1/bookmarks/{questionId}", question.getId())
                        .with(authentication(authOf(user.getId()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOOKMARK001"));
    }

    private Authentication authOf(Long userId) {
        return new UsernamePasswordAuthenticationToken(userId, "test-token", Collections.emptyList());
    }
}
