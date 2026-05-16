package com.socialpulse.app.follow.application.usecase;

import com.socialpulse.app.follow.application.dto.response.FollowersListResponse;

public interface GetFollowersUseCase {
    FollowersListResponse getFollowers(Long userId, Long currentUserId, int page, int size);
}
