package org.example.passpoint.domain.question.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.passpoint.domain.tag.entity.Tag;
import org.example.passpoint.global.entity.BaseEntity;

/**
 * 질문-태그 연결 (N:M 조인 테이블)
 * - (question_id, tag_id) 조합은 유니크: 한 질문에 같은 태그를 중복 연결할 수 없다
 */

@Getter
@Entity
@Table(
        name = "question_tags",
        uniqueConstraints = @UniqueConstraint(columnNames = {"question_id", "tag_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionTag extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @Builder
    private QuestionTag(Question question, Tag tag) {
        this.question = question;
        this.tag = tag;
    }
}