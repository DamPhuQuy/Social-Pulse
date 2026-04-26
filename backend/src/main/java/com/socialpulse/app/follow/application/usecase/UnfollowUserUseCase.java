package com.socialpulse.app.follow.application.usecase;

import com.socialpulse.app.security.user.CustomUserDetails;

public interface UnfollowUserUseCase {
    void unfollowUser(Long followingId, CustomUserDetails currentUser);
}
