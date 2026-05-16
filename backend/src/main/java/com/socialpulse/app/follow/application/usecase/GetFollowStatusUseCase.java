package com.socialpulse.app.follow.application.usecase;

import com.socialpulse.app.follow.application.dto.response.FollowStatusResponse;

public interface GetFollowStatusUseCase {
    FollowStatusResponse getFollowStatus(Long targetUserId, Long viewerUserId);
}
