package org.example.passpoint.domain.answer.service;

import lombok.RequiredArgsConstructor;
import org.example.passpoint.domain.answer.dto.request.AnswerCreateRequest;
import org.example.passpoint.domain.answer.dto.response.AnswerDetailResponse;
import org.example.passpoint.domain.answer.dto.response.AnswerResponse;
import org.example.passpoint.domain.answer.dto.response.AnswerSummaryResponse;
import org.example.passpoint.domain.answer.entity.Answer;
import org.example.passpoint.domain.answer.entity.AnswerStatus;
import org.example.passpoint.domain.answer.entity.AnswerType;
import org.example.passpoint.domain.answer.repository.AnswerRepository;
import org.example.passpoint.domain.feedback.dto.response.FeedbackResponse;
import org.example.passpoint.domain.feedback.entity.Feedback;
import org.example.passpoint.domain.feedback.repository.FeedbackRepository;
import org.example.passpoint.domain.question.entity.MainCategory;
import org.example.passpoint.domain.question.entity.Question;
import org.example.passpoint.domain.question.entity.SubCategory;
import org.example.passpoint.domain.question.repository.QuestionRepository;
import org.example.passpoint.domain.studylog.service.StudyLogService;
import org.example.passpoint.domain.user.entity.User;
import org.example.passpoint.domain.user.repository.UserRepository;
import org.example.passpoint.global.exception.answer.AnswerAccessDeniedException;
import org.example.passpoint.global.s3.S3AudioStorageService;
import org.example.passpoint.global.exception.answer.AnswerNotFoundException;
import org.example.passpoint.global.exception.answer.AnswerTextRequiredException;
import org.example.passpoint.global.exception.answer.AnswerTextTooLongException;
import org.example.passpoint.global.exception.answer.AudioKeyRequiredException;
import org.example.passpoint.global.exception.question.QuestionNotFoundException;
import org.example.passpoint.global.exception.user.UserNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 답변 제출/조회 비즈니스 로직
 * - submit()은 답변을 PENDING으로 저장하고 feedback.requested를 발행한 뒤 즉시 반환한다
 *   (실제 LLM 피드백 생성은 FeedbackWorker가 비동기로 처리)
 */
@Service
@RequiredArgsConstructor
public class AnswerService {

    private static final int ANSWER_TEXT_MAX_LENGTH = 3000;

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final FeedbackRepository feedbackRepository;
    private final AnswerWriteService answerWriteService;
    private final StudyLogService studyLogService;
    private final S3AudioStorageService s3AudioStorageService;

    public AnswerResponse submit(Long userId, AnswerCreateRequest request) {
        // 입력값 검증을 레포지토리 조회보다 먼저 한다 (잘못된 요청에 불필요한 DB 조회를 하지 않도록)
        if (request.type() == AnswerType.VOICE) {
            if (!StringUtils.hasText(request.audioKey())) {
                throw new AudioKeyRequiredException();
            }
        } else {
            if (!StringUtils.hasText(request.answerText())) {
                throw new AnswerTextRequiredException();
            }
            if (request.answerText().length() > ANSWER_TEXT_MAX_LENGTH) {
                throw new AnswerTextTooLongException();
            }
        }

        Question question = questionRepository.findById(request.questionId())
                .orElseThrow(QuestionNotFoundException::new);
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        Answer answer;
        if (request.type() == AnswerType.VOICE) {
            // tx1: 음성 답변 저장(PENDING) + audio.uploaded 발행(AFTER_COMMIT) → SttWorker가 처리
            answer = answerWriteService.createVoiceAnswer(user, question, request.audioKey());
        } else {
            // tx1: 텍스트 답변 저장(PENDING) + feedback.requested 발행(AFTER_COMMIT) → FeedbackWorker가 처리
            answer = answerWriteService.createAnswer(user, question, request.type(), request.answerText());
        }

        studyLogService.recordStudy(userId);
        return AnswerResponse.of(answer);
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

        String audioUrl = (answer.getType() == AnswerType.VOICE && answer.getAudioUrl() != null)
                ? s3AudioStorageService.generateDownloadPresignedUrl(answer.getAudioUrl())
                : null;

        return AnswerDetailResponse.of(answer, feedback, audioUrl);
    }

    /** category가 없으면 전체, 있으면 해당 대분류에 속한 질문의 답변만 조회 (FAILED 제외) */
    @Transactional(readOnly = true)
    public Page<AnswerSummaryResponse> getMyAnswers(Long userId, MainCategory category, Pageable pageable) {
        if (category == null) {
            return toSummaryPage(answerRepository.findByUserIdAndStatusNot(userId, AnswerStatus.FAILED, pageable));
        }

        List<SubCategory> subCategories = Arrays.stream(SubCategory.values())
                .filter(subCategory -> subCategory.getMainCategory() == category)
                .toList();
        return toSummaryPage(answerRepository.findByUserIdAndQuestionSubCategoryInAndStatusNot(userId, subCategories, AnswerStatus.FAILED, pageable));
    }

    /** FAILED 제외 */
    @Transactional(readOnly = true)
    public Page<AnswerSummaryResponse> getMyAnswersByQuestion(Long userId, Long questionId, Pageable pageable) {
        questionRepository.findById(questionId)
                .orElseThrow(QuestionNotFoundException::new);
        return toSummaryPage(answerRepository.findByUserIdAndQuestionIdAndStatusNot(userId, questionId, AnswerStatus.FAILED, pageable));
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
