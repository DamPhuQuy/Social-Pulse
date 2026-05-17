package com.socialpulse.app.follow.application.usecase;

import com.socialpulse.app.follow.application.dto.response.FollowCountsResponse;

public interface GetFollowCountsUseCase {
    FollowCountsResponse getFollowCounts(Long userId);
}
