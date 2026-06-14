package org.example.passpoint.domain.feedback.service;

import lombok.RequiredArgsConstructor;
import org.example.passpoint.domain.answer.entity.Answer;
import org.example.passpoint.domain.answer.entity.AnswerStatus;
import org.example.passpoint.domain.answer.repository.AnswerRepository;
import org.example.passpoint.domain.feedback.dto.response.FeedbackResponse;
import org.example.passpoint.domain.feedback.repository.FeedbackRepository;
import org.example.passpoint.global.exception.answer.AnswerAccessDeniedException;
import org.example.passpoint.global.exception.answer.AnswerNotFoundException;
import org.example.passpoint.global.exception.feedback.FeedbackNotReadyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 피드백 단독 조회 비즈니스 로직
 */
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final AnswerRepository answerRepository;
    private final FeedbackRepository feedbackRepository;

    @Transactional(readOnly = true)
    public FeedbackResponse getFeedback(Long userId, Long answerId) {
        Answer answer = answerRepository.findByIdWithQuestion(answerId)
                .orElseThrow(AnswerNotFoundException::new);

        if (!answer.getUser().getId().equals(userId)) {
            throw new AnswerAccessDeniedException();
        }

        if (answer.getStatus() != AnswerStatus.DONE) {
            throw new FeedbackNotReadyException();
        }

        return feedbackRepository.findByAnswerIdWithQuestion(answerId)
                .map(FeedbackResponse::from)
                .orElseThrow(FeedbackNotReadyException::new);
    }
}
