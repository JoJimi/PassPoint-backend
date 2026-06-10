package org.example.passpoint.domain.question.service;

import lombok.RequiredArgsConstructor;
import org.example.passpoint.domain.question.dto.response.QuestionDetailResponse;
import org.example.passpoint.domain.question.entity.Question;
import org.example.passpoint.domain.question.repository.QuestionRepository;
import org.example.passpoint.global.exception.question.QuestionNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 질문 조회 비즈니스 로직 (PostgreSQL 원본)
 */
@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;

    /** 질문 상세 조회 (정답류 제외) */
    @Transactional(readOnly = true)
    public QuestionDetailResponse getQuestionDetail(Long id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(QuestionNotFoundException::new);
        return QuestionDetailResponse.from(question);
    }
}
