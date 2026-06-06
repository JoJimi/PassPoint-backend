package org.example.passpoint.domain.studylog.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.passpoint.domain.user.entity.User;
import org.example.passpoint.global.entity.BaseEntity;

import java.time.LocalDate;

/**
 * 일자별 학습 기록
 * - (user_id, study_date) 조합은 유니크: 한 사용자는 하루에 학습 기록을 한 행만 가질 수 있다
 */

@Getter
@Entity
@Table(
        name = "study_logs",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "study_date"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudyLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "study_date", nullable = false)
    private LocalDate studyDate;

    @Column(nullable = false)
    private Integer solvedCount;

    @Builder
    private StudyLog(User user, LocalDate studyDate, Integer solvedCount) {
        this.user = user;
        this.studyDate = studyDate;
        this.solvedCount = solvedCount;
    }

    public void increaseSolvedCount() {
        this.solvedCount++;
    }
}