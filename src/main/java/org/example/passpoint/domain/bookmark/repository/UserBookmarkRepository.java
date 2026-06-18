package org.example.passpoint.domain.bookmark.repository;

import org.example.passpoint.domain.bookmark.entity.UserBookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface UserBookmarkRepository extends JpaRepository<UserBookmark, Long> {

    Optional<UserBookmark> findByUserIdAndQuestionId(Long userId, Long questionId);

    boolean existsByUserIdAndQuestionId(Long userId, Long questionId);

    @Query("SELECT b FROM UserBookmark b JOIN FETCH b.question WHERE b.user.id = :userId")
    Page<UserBookmark> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT b.question.id FROM UserBookmark b WHERE b.user.id = :userId AND b.question.id IN :questionIds")
    Set<Long> findBookmarkedQuestionIds(@Param("userId") Long userId, @Param("questionIds") Collection<Long> questionIds);
}
