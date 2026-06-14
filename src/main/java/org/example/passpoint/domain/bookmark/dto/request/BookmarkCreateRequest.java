package org.example.passpoint.domain.bookmark.dto.request;

import jakarta.validation.constraints.NotNull;

public record BookmarkCreateRequest(
        @NotNull Long questionId
) {
}
