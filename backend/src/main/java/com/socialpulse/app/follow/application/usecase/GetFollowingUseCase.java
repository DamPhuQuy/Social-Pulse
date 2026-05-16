package com.socialpulse.app.follow.application.usecase;

import com.socialpulse.app.common.dto.response.PageResponse;
import com.socialpulse.app.user.application.dto.response.UserSummary;

public interface GetFollowingUseCase {
    PageResponse<UserSummary> getFollowing(Long userId, int page, int size);
}
