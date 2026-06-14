package org.example.passpoint.domain.feedback.repository;

import org.example.passpoint.domain.feedback.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    Optional<Feedback> findByAnswerId(Long answerId);

    List<Feedback> findByAnswerIdIn(List<Long> answerIds);
}
