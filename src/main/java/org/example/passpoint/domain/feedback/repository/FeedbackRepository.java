package org.example.passpoint.domain.feedback.repository;

import org.example.passpoint.domain.feedback.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    Optional<Feedback> findByAnswerId(Long answerId);

    List<Feedback> findByAnswerIdIn(List<Long> answerIds);

    @Query("SELECT f FROM Feedback f JOIN FETCH f.answer a JOIN FETCH a.question WHERE a.id = :answerId")
    Optional<Feedback> findByAnswerIdWithQuestion(@Param("answerId") Long answerId);

    @Query("SELECT AVG(f.score) FROM Feedback f WHERE f.answer.user.id = :userId")
    Double findAverageScoreByUserId(@Param("userId") Long userId);

    @Query("SELECT MAX(f.score) FROM Feedback f WHERE f.answer.user.id = :userId")
    Integer findBestScoreByUserId(@Param("userId") Long userId);
}
