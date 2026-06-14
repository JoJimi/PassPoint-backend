package org.example.passpoint.domain.answer.service;

import lombok.RequiredArgsConstructor;
import org.example.passpoint.domain.answer.entity.Answer;
import org.example.passpoint.domain.answer.entity.AnswerStatus;
import org.example.passpoint.domain.answer.entity.AnswerType;
import org.example.passpoint.domain.answer.repository.AnswerRepository;
import org.example.passpoint.domain.feedback.dto.FeedbackResult;
import org.example.passpoint.domain.feedback.entity.Feedback;
import org.example.passpoint.domain.feedback.repository.FeedbackRepository;
import org.example.passpoint.domain.question.entity.Question;
import org.example.passpoint.domain.user.entity.User;
import org.example.passpoint.global.exception.answer.AnswerNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 답변 제출의 tx1/tx2 DB 쓰기 담당
 * - LLM(피드백 생성) 호출은 이 서비스를 거치지 않고 AnswerService에서 트랜잭션 밖에서 수행한다
 */
@Service
@RequiredArgsConstructor
public class AnswerWriteService {

    private final AnswerRepository answerRepository;
    private final FeedbackRepository feedbackRepository;

    /** tx1: 답변 저장 (status = ANALYZING) */
    @Transactional
    public Answer createAnswer(User user, Question question, AnswerType type, String answerText) {
        Answer answer = Answer.builder()
                .user(user)
                .question(question)
                .type(type)
                .answerText(answerText)
                .status(AnswerStatus.ANALYZING)
                .build();
        return answerRepository.save(answer);
    }

    /** tx2: 피드백 저장 + answer.status = DONE */
    @Transactional
    public void completeFeedback(Long answerId, FeedbackResult result) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(AnswerNotFoundException::new);

        Feedback feedback = Feedback.builder()
                .answer(answer)
                .score(result.score())
                .accuracyScore(result.accuracyScore())
                .structureScore(result.structureScore())
                .completenessScore(result.completenessScore())
                .goodPoints(result.goodPoints())
                .improvementPoints(result.improvementPoints())
                .coveredKeywords(result.coveredKeywords())
                .build();
        feedbackRepository.save(feedback);

        answer.updateStatus(AnswerStatus.DONE);
    }

    /** tx2 실패 경로: answer.status = FAILED로 별도 마킹 */
    @Transactional
    public void markFailed(Long answerId) {
        answerRepository.findById(answerId)
                .orElseThrow(AnswerNotFoundException::new)
                .updateStatus(AnswerStatus.FAILED);
    }
}
