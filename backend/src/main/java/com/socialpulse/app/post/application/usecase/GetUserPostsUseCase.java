package com.socialpulse.app.post.application.usecase;

import com.socialpulse.app.common.dto.response.PageResponse;
import com.socialpulse.app.post.application.dto.response.UserPostResponse;
import com.socialpulse.app.security.user.CustomUserDetails;

public interface GetUserPostsUseCase {
    PageResponse<UserPostResponse> getUserPosts(Long userId, int page, int size, CustomUserDetails currentUser);
}
