package org.example.passpoint.domain.question.repository;

import org.example.passpoint.domain.question.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {
}