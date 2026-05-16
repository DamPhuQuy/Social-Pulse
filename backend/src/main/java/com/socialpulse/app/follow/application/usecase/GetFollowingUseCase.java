package com.socialpulse.app.follow.application.usecase;

import com.socialpulse.app.follow.application.dto.response.FollowingListResponse;

public interface GetFollowingUseCase {
    FollowingListResponse getFollowing(Long userId, Long currentUserId, int page, int size);
}
