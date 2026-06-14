package org.example.passpoint.domain.bookmark.dto.response;

import org.example.passpoint.domain.bookmark.entity.UserBookmark;

import java.time.LocalDateTime;

/**
 * 즐겨찾기 등록 응답
 */
public record BookmarkResponse(
        Long bookmarkId,
        Long questionId,
        LocalDateTime createdAt
) {
    public static BookmarkResponse from(UserBookmark bookmark) {
        return new BookmarkResponse(
                bookmark.getId(),
                bookmark.getQuestion().getId(),
                bookmark.getCreatedAt()
        );
    }
}
