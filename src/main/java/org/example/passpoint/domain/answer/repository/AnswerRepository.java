package org.example.passpoint.domain.answer.repository;

import org.example.passpoint.domain.answer.entity.Answer;
import org.example.passpoint.domain.answer.entity.AnswerStatus;
import org.example.passpoint.domain.question.entity.SubCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    @Query("SELECT a FROM Answer a JOIN FETCH a.question WHERE a.id = :id")
    Optional<Answer> findByIdWithQuestion(@Param("id") Long id);

    @Query("SELECT a FROM Answer a JOIN FETCH a.question WHERE a.user.id = :userId")
    Page<Answer> findByUserId(@Param("userId") Long userId, Pageable pageable);

    // mainCategory는 Question에 저장되지 않으므로, 해당 대분류에 속한 subCategory 목록으로 필터링한다
    @Query("SELECT a FROM Answer a JOIN FETCH a.question WHERE a.user.id = :userId AND a.question.subCategory IN :subCategories")
    Page<Answer> findByUserIdAndQuestionSubCategoryIn(@Param("userId") Long userId, @Param("subCategories") List<SubCategory> subCategories, Pageable pageable);

    @Query("SELECT a FROM Answer a JOIN FETCH a.question WHERE a.user.id = :userId AND a.question.id = :questionId")
    Page<Answer> findByUserIdAndQuestionId(@Param("userId") Long userId, @Param("questionId") Long questionId, Pageable pageable);

    // FAILED(처리 실패)는 통계에서 제외
    long countByUserIdAndStatusNot(Long userId, AnswerStatus status);
}
