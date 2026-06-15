package org.example.passpoint.domain.question.repository.search;

import org.example.passpoint.TestcontainersConfiguration;
import org.example.passpoint.domain.question.document.QuestionDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ES 질문 검색 통합 테스트 (Testcontainers + Nori 한글 형태소 분석)
 * - QuestionSearchRepository.searchQuestions()의 키워드 multi-match + 카테고리 필터 검증
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class QuestionSearchIntegrationTest {

    @Autowired
    private QuestionSearchRepository questionSearchRepository;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    private static final Long GC_QUESTION_ID = 1L;
    private static final Long HTTP_QUESTION_ID = 2L;

    @BeforeEach
    void setUp() {
        // QuestionDataInitializer가 앱 시작 시 시드한 질문들이 ES 인덱스에 남아있으므로
        // 테스트 격리를 위해 먼저 비운다.
        questionSearchRepository.deleteAll();

        questionSearchRepository.saveAll(List.of(
                QuestionDocument.builder()
                        .id(GC_QUESTION_ID)
                        .title("가비지 컬렉션이란 무엇인가요?")
                        .content("JVM에서 가비지 컬렉션이 동작하는 방식을 설명하시오.")
                        .mainCategory("LANGUAGE")
                        .subCategory("JVM")
                        .difficulty("MEDIUM")
                        .tags(List.of())
                        .createdAt(LocalDateTime.now())
                        .build(),
                QuestionDocument.builder()
                        .id(HTTP_QUESTION_ID)
                        .title("HTTP와 HTTPS의 차이는?")
                        .content("HTTP와 HTTPS의 차이를 설명하시오.")
                        .mainCategory("WEB")
                        .subCategory("HTTP")
                        .difficulty("MEDIUM")
                        .tags(List.of())
                        .createdAt(LocalDateTime.now())
                        .build()
        ));

        elasticsearchOperations.indexOps(QuestionDocument.class).refresh();
    }

    @AfterEach
    void tearDown() {
        questionSearchRepository.deleteAll();
    }

    @Test
    void 공백없이검색해도_Nori형태소분석으로_가비지컬렉션문서가검색된다() {
        Page<QuestionDocument> result = questionSearchRepository.searchQuestions("가비지컬렉션", null, PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(QuestionDocument::getId).containsExactly(GC_QUESTION_ID);
    }

    @Test
    void 형태소단위키워드만으로도_가비지컬렉션문서가검색된다() {
        Page<QuestionDocument> result = questionSearchRepository.searchQuestions("컬렉션", null, PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(QuestionDocument::getId).containsExactly(GC_QUESTION_ID);
    }

    @Test
    void 카테고리필터를_적용하면_해당카테고리문서만조회된다() {
        Page<QuestionDocument> result = questionSearchRepository.searchQuestions(null, "WEB", PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(QuestionDocument::getId).containsExactly(HTTP_QUESTION_ID);
    }

    @Test
    void 키워드와카테고리를_동시에적용할수있다() {
        Page<QuestionDocument> result = questionSearchRepository.searchQuestions("차이", "WEB", PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(QuestionDocument::getId).containsExactly(HTTP_QUESTION_ID);
    }

    @Test
    void 일치하는키워드가없으면_빈결과를반환한다() {
        Page<QuestionDocument> result = questionSearchRepository.searchQuestions("쿠버네티스", null, PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }
}
