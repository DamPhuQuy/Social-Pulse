package com.socialpulse.app.follow.application.usecase;

import com.socialpulse.app.follow.application.dto.response.FollowResponse;
import com.socialpulse.app.security.user.CustomUserDetails;

public interface FollowUserUseCase {
    FollowResponse followUser(Long followingId, CustomUserDetails currentUser);
}
