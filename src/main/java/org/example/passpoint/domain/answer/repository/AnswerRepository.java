package org.example.passpoint.domain.answer.repository;

import org.example.passpoint.domain.answer.entity.Answer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    @Query("SELECT a FROM Answer a JOIN FETCH a.question WHERE a.id = :id")
    Optional<Answer> findByIdWithQuestion(@Param("id") Long id);

    @Query("SELECT a FROM Answer a JOIN FETCH a.question WHERE a.user.id = :userId")
    Page<Answer> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT a FROM Answer a JOIN FETCH a.question WHERE a.user.id = :userId AND a.question.id = :questionId")
    Page<Answer> findByUserIdAndQuestionId(@Param("userId") Long userId, @Param("questionId") Long questionId, Pageable pageable);

    @Query("SELECT COUNT(a) FROM Answer a WHERE a.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);
}
