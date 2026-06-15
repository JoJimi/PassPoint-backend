package org.example.passpoint.domain.feedback.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * SpringAiFeedbackGenerator.filterKeywords()의 keywordPool 교집합 필터(환각 방지) 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class SpringAiFeedbackGeneratorTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    private SpringAiFeedbackGenerator feedbackGenerator;

    @BeforeEach
    void setUp() {
        given(chatClientBuilder.build()).willReturn(mock(ChatClient.class));
        feedbackGenerator = new SpringAiFeedbackGenerator(chatClientBuilder);
    }

    @Test
    void coveredKeywords가_keywordPool에있으면_그대로유지된다() {
        List<String> covered = List.of("HTTPS", "SSL/TLS");
        List<String> keywordPool = List.of("HTTPS", "SSL/TLS", "암호화");

        List<String> result = feedbackGenerator.filterKeywords(covered, keywordPool);

        assertThat(result).containsExactly("HTTPS", "SSL/TLS");
    }

    @Test
    void coveredKeywords중_keywordPool에없는것은_제거된다() {
        List<String> covered = List.of("HTTPS", "TCP");
        List<String> keywordPool = List.of("HTTPS", "SSL/TLS", "암호화");

        List<String> result = feedbackGenerator.filterKeywords(covered, keywordPool);

        assertThat(result).containsExactly("HTTPS");
    }

    @Test
    void 대소문자와공백이달라도_keywordPool표기로매칭된다() {
        List<String> covered = List.of(" https ", "ssl/tls");
        List<String> keywordPool = List.of("HTTPS", "SSL/TLS", "암호화");

        List<String> result = feedbackGenerator.filterKeywords(covered, keywordPool);

        assertThat(result).containsExactly("HTTPS", "SSL/TLS");
    }

    @Test
    void covered가null이면_빈리스트를반환한다() {
        List<String> result = feedbackGenerator.filterKeywords(null, List.of("HTTPS"));

        assertThat(result).isEmpty();
    }

    @Test
    void keywordPool이null이면_빈리스트를반환한다() {
        List<String> result = feedbackGenerator.filterKeywords(List.of("HTTPS"), null);

        assertThat(result).isEmpty();
    }

    @Test
    void 중복된coveredKeywords는_한번만포함된다() {
        List<String> covered = List.of("HTTPS", "https", "HTTPS");
        List<String> keywordPool = List.of("HTTPS", "SSL/TLS");

        List<String> result = feedbackGenerator.filterKeywords(covered, keywordPool);

        assertThat(result).containsExactly("HTTPS");
    }
}
