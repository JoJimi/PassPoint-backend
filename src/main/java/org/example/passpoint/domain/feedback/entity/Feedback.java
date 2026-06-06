package org.example.passpoint.domain.feedback.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.passpoint.domain.answer.entity.Answer;
import org.example.passpoint.global.entity.BaseEntity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

/**
 * AI 피드백
 * - answer_id는 유니크: 한 답변당 피드백은 하나만 존재한다 (1:1)
 * - score(총점)는 세부 점수(accuracy/structure/completeness)로 서버에서 계산
 */

@Getter
@Entity
@Table(name = "feedbacks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Feedback extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id", nullable = false, unique = true)
    private Answer answer;

    @Column(nullable = false)
    private Integer score;            // 총점 (서버 계산)

    @Column(nullable = false)
    private Integer accuracyScore;

    @Column(nullable = false)
    private Integer structureScore;

    @Column(nullable = false)
    private Integer completenessScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> goodPoints;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> improvementPoints;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> coveredKeywords;

    @Builder
    private Feedback(Answer answer, Integer score, Integer accuracyScore, Integer structureScore,
                     Integer completenessScore, List<String> goodPoints,
                     List<String> improvementPoints, List<String> coveredKeywords) {
        this.answer = answer;
        this.score = score;
        this.accuracyScore = accuracyScore;
        this.structureScore = structureScore;
        this.completenessScore = completenessScore;
        this.goodPoints = goodPoints;
        this.improvementPoints = improvementPoints;
        this.coveredKeywords = coveredKeywords;
    }
}