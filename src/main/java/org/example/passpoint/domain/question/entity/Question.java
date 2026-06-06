package org.example.passpoint.domain.question.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.passpoint.global.entity.BaseEntity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

/**
 * 질문(문제)
 * - guidePoints, keywordPool은 jsonb 컬럼에 List로 저장
 * - modelAnswer, keywordPool은 채점에만 사용 (질문 상세 응답에서 제외 → 정답 노출 방지)
 */

@Getter
@Entity
@Table(name = "questions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Question extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubCategory subCategory;             // 네트워크/운영체제/DB/API...

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(columnDefinition = "TEXT")
    private String hint;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> guidePoints;     // 답변 가이드 / 채점 루브릭

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> keywordPool;     // 인정 키워드 목록

    @Column(columnDefinition = "TEXT")
    private String modelAnswer;

    @Builder
    private Question(SubCategory subCategory, Difficulty difficulty,
                     String title, String content, String hint,
                     List<String> guidePoints, List<String> keywordPool, String modelAnswer) {
        this.subCategory = subCategory;
        this.difficulty = difficulty;
        this.title = title;
        this.content = content;
        this.hint = hint;
        this.guidePoints = guidePoints;
        this.keywordPool = keywordPool;
        this.modelAnswer = modelAnswer;
    }

    // mainCategory는 저장하지 않고 subCategory에서 유도
    public MainCategory getMainCategory() {
        return subCategory.getMainCategory();
    }
}