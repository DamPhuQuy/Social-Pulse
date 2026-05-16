package com.socialpulse.app.bookmark.application.usecase;

import com.socialpulse.app.common.dto.response.PageResponse;
import com.socialpulse.app.post.application.dto.response.UserPostResponse;
import com.socialpulse.app.security.user.CustomUserDetails;

public interface GetBookmarksUseCase {
    PageResponse<UserPostResponse> getBookmarks(int page, int size, CustomUserDetails currentUser);
}
