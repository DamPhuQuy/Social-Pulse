package com.socialpulse.app.bookmark.application.usecase;

import com.socialpulse.app.bookmark.application.dto.response.BookmarkResponse;
import com.socialpulse.app.security.user.CustomUserDetails;

public interface CreateBookmarkUseCase {
    BookmarkResponse createBookmark(Long postId, CustomUserDetails currentUser);
}
