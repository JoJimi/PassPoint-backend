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

    // 이력 목록에서는 FAILED(처리 실패) 답변을 노출하지 않는다 (GET /answers/{id} 단건 조회는 폴링 계약상 FAILED도 그대로 보여줘야 하므로 영향 없음)
    @Query("SELECT a FROM Answer a JOIN FETCH a.question WHERE a.user.id = :userId AND a.status <> :excludedStatus")
    Page<Answer> findByUserIdAndStatusNot(@Param("userId") Long userId, @Param("excludedStatus") AnswerStatus excludedStatus, Pageable pageable);

    // mainCategory는 Question에 저장되지 않으므로, 해당 대분류에 속한 subCategory 목록으로 필터링한다
    @Query("SELECT a FROM Answer a JOIN FETCH a.question WHERE a.user.id = :userId AND a.question.subCategory IN :subCategories AND a.status <> :excludedStatus")
    Page<Answer> findByUserIdAndQuestionSubCategoryInAndStatusNot(@Param("userId") Long userId, @Param("subCategories") List<SubCategory> subCategories, @Param("excludedStatus") AnswerStatus excludedStatus, Pageable pageable);

    @Query("SELECT a FROM Answer a JOIN FETCH a.question WHERE a.user.id = :userId AND a.question.id = :questionId AND a.status <> :excludedStatus")
    Page<Answer> findByUserIdAndQuestionIdAndStatusNot(@Param("userId") Long userId, @Param("questionId") Long questionId, @Param("excludedStatus") AnswerStatus excludedStatus, Pageable pageable);

    // FAILED(처리 실패)는 통계에서 제외
    long countByUserIdAndStatusNot(Long userId, AnswerStatus status);
}
