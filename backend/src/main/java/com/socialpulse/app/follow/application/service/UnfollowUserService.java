package com.socialpulse.app.follow.application.service;

import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.follow.application.usecase.UnfollowUserUseCase;
import com.socialpulse.app.follow.domain.repository.FollowRepository;
import com.socialpulse.app.security.user.CustomUserDetails;

public class UnfollowUserService implements UnfollowUserUseCase {

    private final FollowRepository followRepository;

    public UnfollowUserService(FollowRepository followRepository) {
        this.followRepository = followRepository;
    }

    @Override
    @Transactional
    public void unfollowUser(Long followingId, CustomUserDetails currentUser) {
        if (!followRepository.existsByFollowerIdAndFollowingId(currentUser.getId(), followingId)) {
            throw new AppException(UserCode.NOT_FOLLOWING);
        }

        followRepository.deleteByFollowerIdAndFollowingId(currentUser.getId(), followingId);
    }
}
