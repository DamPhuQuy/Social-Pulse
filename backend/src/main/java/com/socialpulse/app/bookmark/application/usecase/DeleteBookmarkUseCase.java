package com.socialpulse.app.bookmark.application.usecase;

import com.socialpulse.app.security.user.CustomUserDetails;

public interface DeleteBookmarkUseCase {
    void deleteBookmark(Long postId, CustomUserDetails currentUser);
}
