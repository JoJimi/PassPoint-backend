package org.example.passpoint.domain.bookmark.service;

import lombok.RequiredArgsConstructor;
import org.example.passpoint.domain.bookmark.dto.response.BookmarkResponse;
import org.example.passpoint.domain.bookmark.dto.response.BookmarkSummaryResponse;
import org.example.passpoint.domain.bookmark.entity.UserBookmark;
import org.example.passpoint.domain.bookmark.repository.UserBookmarkRepository;
import org.example.passpoint.domain.question.entity.Question;
import org.example.passpoint.domain.question.repository.QuestionRepository;
import org.example.passpoint.domain.user.entity.User;
import org.example.passpoint.domain.user.repository.UserRepository;
import org.example.passpoint.global.exception.bookmark.BookmarkNotFoundException;
import org.example.passpoint.global.exception.question.QuestionNotFoundException;
import org.example.passpoint.global.exception.user.UserNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 즐겨찾기 등록/삭제/조회 비즈니스 로직
 */
@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final UserBookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;

    /** 즐겨찾기 등록 (멱등 - 이미 등록돼 있으면 기존 항목을 반환) */
    @Transactional
    public BookmarkResponse addBookmark(Long userId, Long questionId) {
        return bookmarkRepository.findByUserIdAndQuestionId(userId, questionId)
                .map(BookmarkResponse::from)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(UserNotFoundException::new);
                    Question question = questionRepository.findById(questionId)
                            .orElseThrow(QuestionNotFoundException::new);

                    UserBookmark bookmark = UserBookmark.builder()
                            .user(user)
                            .question(question)
                            .build();
                    return BookmarkResponse.from(bookmarkRepository.save(bookmark));
                });
    }

    /** 즐겨찾기 삭제 */
    @Transactional
    public void removeBookmark(Long userId, Long questionId) {
        UserBookmark bookmark = bookmarkRepository.findByUserIdAndQuestionId(userId, questionId)
                .orElseThrow(BookmarkNotFoundException::new);
        bookmarkRepository.delete(bookmark);
    }

    @Transactional(readOnly = true)
    public Page<BookmarkSummaryResponse> getMyBookmarks(Long userId, Pageable pageable) {
        return bookmarkRepository.findByUserId(userId, pageable)
                .map(BookmarkSummaryResponse::from);
    }
}
