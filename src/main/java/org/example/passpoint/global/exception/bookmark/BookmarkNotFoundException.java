package org.example.passpoint.global.exception.bookmark;

import org.example.passpoint.global.exception.BusinessException;
import org.example.passpoint.global.exception.ErrorCode;

/** 즐겨찾기를 찾을 수 없을 때 */
public class BookmarkNotFoundException extends BusinessException {
    public BookmarkNotFoundException() {
        super(ErrorCode.BOOKMARK_NOT_FOUND);
    }
}
