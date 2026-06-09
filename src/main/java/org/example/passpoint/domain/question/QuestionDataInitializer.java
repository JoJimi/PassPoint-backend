package org.example.passpoint.domain.question;

import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.passpoint.domain.question.document.QuestionDocument;
import org.example.passpoint.domain.question.dto.QuestionSeedData;
import org.example.passpoint.domain.question.entity.Question;
import org.example.passpoint.domain.question.repository.QuestionRepository;
import org.example.passpoint.domain.question.repository.search.QuestionSearchRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;

/**
 * 서버 시작 시 questions.json을 읽어 사전 질문을 PostgreSQL + ES에 시드
 * - questions 테이블이 비어있을 때만 삽입 (재시작 시 중복 방지)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionDataInitializer implements ApplicationRunner {

    private final QuestionRepository questionRepository;
    private final QuestionSearchRepository questionSearchRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        // 이미 질문이 있으면 시드 스킵 (중복 방지)
        if (questionRepository.count() > 0) {
            log.info("질문 데이터가 이미 존재하여 시드를 건너뜁니다.");
            return;
        }

        // questions.json 읽기
        QuestionSeedData[] seedArray;
        try (InputStream is = new ClassPathResource("data/questions.json").getInputStream()) {
            seedArray = objectMapper.readValue(is, QuestionSeedData[].class);
        }

        List<QuestionSeedData> seeds = List.of(seedArray);

        // 각 질문을 PostgreSQL 저장 + ES 색인
        for (QuestionSeedData seed : seeds) {
            Question question = Question.builder()
                    .subCategory(seed.subCategory())
                    .difficulty(seed.difficulty())
                    .title(seed.title())
                    .content(seed.content())
                    .hint(seed.hint())
                    .guidePoints(seed.guidePoints())
                    .keywordPool(seed.keywordPool())
                    .modelAnswer(seed.modelAnswer())
                    .build();

            Question saved = questionRepository.save(question);             // PostgreSQL
            questionSearchRepository.save(QuestionDocument.from(saved));    // ES 색인
        }

        log.info("질문 {} 건 시드 완료 (PostgreSQL + ES)", seeds.size());
    }

}
