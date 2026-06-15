package org.example.passpoint.domain.feedback.service;

import org.example.passpoint.TestcontainersConfiguration;
import org.example.passpoint.domain.feedback.dto.FeedbackResult;
import org.example.passpoint.domain.question.entity.Difficulty;
import org.example.passpoint.domain.question.entity.Question;
import org.example.passpoint.domain.question.entity.SubCategory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SpringAiFeedbackGenerator의 실제 OpenAI 연동 확인용 테스트
 * - 실제 OpenAI API를 호출하므로 비용이 발생하고 응답이 비결정적이다.
 * - 평소 빌드/CI에서는 실행하지 않고, OpenAI 연동을 직접 확인할 때만 @Disabled를 제거하고 수동으로 실행한다.
 */
@Disabled("실제 OpenAI API를 호출하는 수동 확인용 테스트 - 평소 빌드에서는 실행하지 않는다")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SpringAiFeedbackGeneratorIntegrationTest {

    @Autowired
    private SpringAiFeedbackGenerator feedbackGenerator;

    @Test
    void 실제OpenAI호출로_피드백을생성한다() {
        Question question = Question.builder()
                .subCategory(SubCategory.HTTP)
                .difficulty(Difficulty.MEDIUM)
                .title("HTTP와 HTTPS의 차이는?")
                .content("HTTP와 HTTPS의 차이를 설명하시오.")
                .guidePoints(List.of("암호화", "포트 번호"))
                .keywordPool(List.of("HTTPS", "SSL/TLS", "암호화", "포트"))
                .modelAnswer("HTTPS는 SSL/TLS로 암호화된 HTTP로, 기본 포트는 443번을 사용한다.")
                .build();

        FeedbackResult result = feedbackGenerator.generate(
                question, "HTTPS는 SSL/TLS를 이용해 HTTP 통신을 암호화한 것입니다.");

        assertThat(result.score()).isBetween(0, 100);
        assertThat(result.accuracyScore()).isBetween(0, 100);
        assertThat(result.structureScore()).isBetween(0, 100);
        assertThat(result.completenessScore()).isBetween(0, 100);
        assertThat(result.goodPoints()).isNotEmpty();
        assertThat(result.coveredKeywords()).isSubsetOf(question.getKeywordPool());
    }
}
