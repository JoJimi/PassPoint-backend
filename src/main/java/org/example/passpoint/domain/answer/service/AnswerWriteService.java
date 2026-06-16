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
import org.example.passpoint.global.kafka.KafkaPublishEvent;
import org.example.passpoint.global.kafka.KafkaTopics;
import org.example.passpoint.global.kafka.event.AudioUploadedEvent;
import org.example.passpoint.global.kafka.event.FeedbackCompletedEvent;
import org.example.passpoint.global.kafka.event.FeedbackRequestedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 답변 제출/처리의 DB 쓰기 + 이벤트 발행 담당
 * - Kafka 발행은 트랜잭션 안에서 KafkaPublishEvent를 publishEvent()로 발행하고,
 *   AfterCommitKafkaPublisher가 AFTER_COMMIT 시점에 실제로 발행한다 (dual-write 대응)
 * - LLM(피드백 생성) 호출은 이 서비스를 거치지 않고 트랜잭션 밖에서 수행한다
 */
@Service
@RequiredArgsConstructor
public class AnswerWriteService {

    private final AnswerRepository answerRepository;
    private final FeedbackRepository feedbackRepository;
    private final ApplicationEventPublisher eventPublisher;

    /** tx1: 답변 저장 (status = PENDING) + feedback.requested 발행(AFTER_COMMIT) */
    @Transactional
    public Answer createAnswer(User user, Question question, AnswerType type, String answerText) {
        Answer answer = Answer.builder()
                .user(user)
                .question(question)
                .type(type)
                .answerText(answerText)
                .status(AnswerStatus.PENDING)
                .build();
        answerRepository.save(answer);

        eventPublisher.publishEvent(new KafkaPublishEvent(
                KafkaTopics.FEEDBACK_REQUESTED, answer.getId().toString(), new FeedbackRequestedEvent(answer.getId())));

        return answer;
    }

    /** tx1: 음성 답변 저장 (status = PENDING) + audio.uploaded 발행(AFTER_COMMIT) */
    @Transactional
    public Answer createVoiceAnswer(User user, Question question, String audioKey) {
        Answer answer = Answer.builder()
                .user(user)
                .question(question)
                .type(AnswerType.VOICE)
                .audioUrl(audioKey)
                .status(AnswerStatus.PENDING)
                .build();
        answerRepository.save(answer);

        eventPublisher.publishEvent(new KafkaPublishEvent(
                KafkaTopics.AUDIO_UPLOADED, answer.getId().toString(), new AudioUploadedEvent(answer.getId())));

        return answer;
    }

    /** SttWorker 진입 시 status 전이: PENDING -> TRANSCRIBING (멱등 체크 포함) */
    @Transactional
    public Answer markTranscribing(Long answerId) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(AnswerNotFoundException::new);

        if (answer.getStatus() != AnswerStatus.PENDING) {
            return null;
        }

        answer.updateStatus(AnswerStatus.TRANSCRIBING);
        return answer;
    }

    /** tx2(STT): 변환 결과 저장 + feedback.requested 발행(AFTER_COMMIT) */
    @Transactional
    public void completeTranscription(Long answerId, String transcript) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(AnswerNotFoundException::new);

        answer.updateTranscript(transcript);

        eventPublisher.publishEvent(new KafkaPublishEvent(
                KafkaTopics.FEEDBACK_REQUESTED, answerId.toString(), new FeedbackRequestedEvent(answerId)));
    }

    /**
     * FeedbackWorker 진입 시 status 전이: PENDING/TRANSCRIBING -> ANALYZING
     * - TEXT 답변은 PENDING으로, VOICE 답변은 STT 완료 후 TRANSCRIBING으로 합류한다
     * - 이미 ANALYZING/DONE/FAILED면 null 반환 (멱등)
     * - LLM 호출에 필요한 question을 함께 fetch해 트랜잭션 밖에서도 사용할 수 있게 한다
     */
    @Transactional
    public Answer markAnalyzing(Long answerId) {
        Answer answer = answerRepository.findByIdWithQuestion(answerId)
                .orElseThrow(AnswerNotFoundException::new);

        if (answer.getStatus() != AnswerStatus.PENDING && answer.getStatus() != AnswerStatus.TRANSCRIBING) {
            return null;
        }

        answer.updateStatus(AnswerStatus.ANALYZING);
        return answer;
    }

    /** tx2: 피드백 저장 + answer.status = DONE + feedback.completed 발행(AFTER_COMMIT) */
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

        eventPublisher.publishEvent(new KafkaPublishEvent(
                KafkaTopics.FEEDBACK_COMPLETED, answerId.toString(), new FeedbackCompletedEvent(answerId)));
    }

    /** tx2 실패 경로: answer.status = FAILED로 별도 마킹 */
    @Transactional
    public void markFailed(Long answerId) {
        answerRepository.findById(answerId)
                .orElseThrow(AnswerNotFoundException::new)
                .updateStatus(AnswerStatus.FAILED);
    }
}
