package org.example.passpoint.domain.bookmark.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.passpoint.domain.bookmark.dto.request.BookmarkCreateRequest;
import org.example.passpoint.domain.bookmark.dto.response.BookmarkResponse;
import org.example.passpoint.domain.bookmark.dto.response.BookmarkSummaryResponse;
import org.example.passpoint.domain.bookmark.service.BookmarkService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 즐겨찾기 등록/삭제/조회 API
 */
@Tag(name = "Bookmark", description = "즐겨찾기 API")
@RestController
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    /** 즐겨찾기 등록 (멱등 - 이미 등록돼 있으면 기존 항목을 그대로 반환) */
    @Operation(summary = "즐겨찾기 등록", description = "질문을 즐겨찾기에 추가한다. 이미 등록돼 있으면 기존 항목을 그대로 반환한다.")
    @PostMapping("/api/v1/bookmarks")
    public ResponseEntity<BookmarkResponse> addBookmark(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody BookmarkCreateRequest request) {
        return ResponseEntity.ok(bookmarkService.addBookmark(userId, request.questionId()));
    }

    /** 즐겨찾기 삭제 */
    @Operation(summary = "즐겨찾기 삭제", description = "질문을 즐겨찾기에서 제거한다. 미등록 상태면 404 BOOKMARK_NOT_FOUND.")
    @DeleteMapping("/api/v1/bookmarks/{questionId}")
    public ResponseEntity<Void> removeBookmark(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long questionId) {
        bookmarkService.removeBookmark(userId, questionId);
        return ResponseEntity.noContent().build();
    }

    /** 즐겨찾기한 질문 목록 (페이징) */
    @Operation(summary = "즐겨찾기 목록", description = "내가 즐겨찾기한 질문 목록을 페이징으로 조회한다.")
    @GetMapping("/api/v1/bookmarks")
    public Page<BookmarkSummaryResponse> getMyBookmarks(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return bookmarkService.getMyBookmarks(userId, pageable);
    }
}
