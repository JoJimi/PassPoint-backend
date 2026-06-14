package org.example.passpoint.domain.answer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.passpoint.domain.answer.dto.request.AnswerCreateRequest;
import org.example.passpoint.domain.answer.dto.response.AnswerDetailResponse;
import org.example.passpoint.domain.answer.dto.response.AnswerResponse;
import org.example.passpoint.domain.answer.dto.response.AnswerSummaryResponse;
import org.example.passpoint.domain.answer.entity.Answer;
import org.example.passpoint.domain.answer.entity.AnswerStatus;
import org.example.passpoint.domain.answer.entity.AnswerType;
import org.example.passpoint.domain.answer.repository.AnswerRepository;
import org.example.passpoint.domain.feedback.dto.FeedbackResult;
import org.example.passpoint.domain.feedback.dto.response.FeedbackResponse;
import org.example.passpoint.domain.feedback.entity.Feedback;
import org.example.passpoint.domain.feedback.repository.FeedbackRepository;
import org.example.passpoint.domain.feedback.service.FeedbackGenerator;
import org.example.passpoint.domain.question.entity.Question;
import org.example.passpoint.domain.question.repository.QuestionRepository;
import org.example.passpoint.domain.user.entity.User;
import org.example.passpoint.domain.user.repository.UserRepository;
import org.example.passpoint.global.exception.answer.AnswerAccessDeniedException;
import org.example.passpoint.global.exception.answer.AnswerNotFoundException;
import org.example.passpoint.global.exception.answer.AnswerTextRequiredException;
import org.example.passpoint.global.exception.answer.AnswerTextTooLongException;
import org.example.passpoint.global.exception.answer.VoiceNotSupportedException;
import org.example.passpoint.global.exception.question.QuestionNotFoundException;
import org.example.passpoint.global.exception.user.UserNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 답변 제출/조회 비즈니스 로직
 * - submit()은 tx1(답변 저장) -> LLM 호출(트랜잭션 밖) -> tx2(피드백 저장+상태 전이) 순으로 진행한다
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerService {

    private static final int ANSWER_TEXT_MAX_LENGTH = 3000;

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final FeedbackRepository feedbackRepository;
    private final FeedbackGenerator feedbackGenerator;
    private final AnswerWriteService answerWriteService;

    public AnswerResponse submit(Long userId, AnswerCreateRequest request) {
        validateAnswerText(request.type(), request.answerText());

        Question question = questionRepository.findById(request.questionId())
                .orElseThrow(QuestionNotFoundException::new);
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        // tx1: 답변 저장 (status = ANALYZING)
        Answer answer = answerWriteService.createAnswer(user, question, request.type(), request.answerText());

        // 트랜잭션 밖: 피드백 생성 (LLM 호출 자리, 2주차는 더미)
        AnswerStatus finalStatus;
        try {
            FeedbackResult result = feedbackGenerator.generate(question, request.answerText());
            // tx2: 피드백 저장 + status = DONE
            answerWriteService.completeFeedback(answer.getId(), result);
            finalStatus = AnswerStatus.DONE;
        } catch (Exception e) {
            log.error("피드백 생성 실패: answerId={}", answer.getId(), e);
            // tx2: status = FAILED로 별도 마킹
            answerWriteService.markFailed(answer.getId());
            finalStatus = AnswerStatus.FAILED;
        }

        return AnswerResponse.of(answer, finalStatus);
    }

    @Transactional(readOnly = true)
    public AnswerDetailResponse getAnswerDetail(Long userId, Long answerId) {
        Answer answer = answerRepository.findByIdWithQuestion(answerId)
                .orElseThrow(AnswerNotFoundException::new);

        if (!answer.getUser().getId().equals(userId)) {
            throw new AnswerAccessDeniedException();
        }

        FeedbackResponse feedback = (answer.getStatus() == AnswerStatus.DONE)
                ? feedbackRepository.findByAnswerId(answerId).map(FeedbackResponse::from).orElse(null)
                : null;

        return AnswerDetailResponse.of(answer, feedback);
    }

    @Transactional(readOnly = true)
    public Page<AnswerSummaryResponse> getMyAnswers(Long userId, Pageable pageable) {
        return toSummaryPage(answerRepository.findByUserId(userId, pageable));
    }

    @Transactional(readOnly = true)
    public Page<AnswerSummaryResponse> getMyAnswersByQuestion(Long userId, Long questionId, Pageable pageable) {
        questionRepository.findById(questionId)
                .orElseThrow(QuestionNotFoundException::new);
        return toSummaryPage(answerRepository.findByUserIdAndQuestionId(userId, questionId, pageable));
    }

    private void validateAnswerText(AnswerType type, String answerText) {
        if (type == AnswerType.VOICE) {
            throw new VoiceNotSupportedException();
        }
        if (!StringUtils.hasText(answerText)) {
            throw new AnswerTextRequiredException();
        }
        if (answerText.length() > ANSWER_TEXT_MAX_LENGTH) {
            throw new AnswerTextTooLongException();
        }
    }

    private Page<AnswerSummaryResponse> toSummaryPage(Page<Answer> answers) {
        List<Long> answerIds = answers.getContent().stream()
                .map(Answer::getId)
                .toList();

        Map<Long, Integer> scoreByAnswerId = feedbackRepository.findByAnswerIdIn(answerIds).stream()
                .collect(Collectors.toMap(feedback -> feedback.getAnswer().getId(), Feedback::getScore));

        return answers.map(answer -> AnswerSummaryResponse.of(answer, scoreByAnswerId.get(answer.getId())));
    }
}
