package org.example.passpoint.domain.bookmark.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.passpoint.domain.question.entity.Question;
import org.example.passpoint.domain.user.entity.User;
import org.example.passpoint.global.entity.BaseEntity;

/**
 * 즐겨찾기
 * - (user_id, question_id) 조합은 유니크: 한 사용자가 같은 질문을 중복 즐겨찾기할 수 없다
 */

@Getter
@Entity
@Table(
        name = "user_bookmarks",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "question_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBookmark extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Builder
    private UserBookmark(User user, Question question) {
        this.user = user;
        this.question = question;
    }
}