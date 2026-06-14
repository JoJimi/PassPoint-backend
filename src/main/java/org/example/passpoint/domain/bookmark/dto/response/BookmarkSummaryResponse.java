package org.example.passpoint.domain.bookmark.dto.response;

import org.example.passpoint.domain.bookmark.entity.UserBookmark;

import java.time.LocalDateTime;

/**
 * 즐겨찾기한 질문 목록 응답
 */
public record BookmarkSummaryResponse(
        Long questionId,
        String title,
        String mainCategory,
        LocalDateTime bookmarkedAt
) {
    public static BookmarkSummaryResponse from(UserBookmark bookmark) {
        return new BookmarkSummaryResponse(
                bookmark.getQuestion().getId(),
                bookmark.getQuestion().getTitle(),
                bookmark.getQuestion().getMainCategory().name(),
                bookmark.getCreatedAt()
        );
    }
}
