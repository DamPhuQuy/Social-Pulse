package com.socialpulse.app.discovery.application.usecase;

import com.socialpulse.app.common.dto.response.PageResponse;
import com.socialpulse.app.post.application.dto.response.UserPostResponse;

public interface SearchPostsUseCase {
    PageResponse<UserPostResponse> searchPosts(String query, int page, int size);
}
