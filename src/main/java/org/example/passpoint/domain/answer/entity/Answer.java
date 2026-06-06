package org.example.passpoint.domain.answer.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.passpoint.domain.question.entity.Question;
import org.example.passpoint.domain.user.entity.User;
import org.example.passpoint.global.entity.BaseEntity;

/**
 * 답변
 * - type이 TEXT면 answerText, VOICE면 audioUrl/audioDuration 사용
 * - status: PENDING → TRANSCRIBING(음성만) → ANALYZING → DONE / FAILED
 */

@Getter
@Entity
@Table(name = "answers")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Answer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnswerType type;

    @Column(columnDefinition = "TEXT")
    private String answerText;        // 텍스트 또는 STT 변환 결과

    private String audioUrl;          // 음성 S3 키 (음성일 때만)

    private Integer audioDuration;    // 녹음 길이(초)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnswerStatus status;

    @Builder
    private Answer(User user, Question question, AnswerType type,
                   String answerText, String audioUrl, Integer audioDuration, AnswerStatus status) {
        this.user = user;
        this.question = question;
        this.type = type;
        this.answerText = answerText;
        this.audioUrl = audioUrl;
        this.audioDuration = audioDuration;
        this.status = status;
    }

    // 상태 전이 (비동기 파이프라인에서 사용)
    public void updateStatus(AnswerStatus status) {
        this.status = status;
    }

    // STT 변환 결과 반영
    public void updateTranscript(String answerText) {
        this.answerText = answerText;
    }
}