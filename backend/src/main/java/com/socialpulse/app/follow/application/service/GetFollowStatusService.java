package com.socialpulse.app.follow.application.service;

import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.follow.application.dto.response.FollowStatusResponse;
import com.socialpulse.app.follow.application.usecase.GetFollowStatusUseCase;
import com.socialpulse.app.follow.domain.repository.FollowRepository;
import com.socialpulse.app.user.domain.repository.UserRepository;

public class GetFollowStatusService implements GetFollowStatusUseCase {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    public GetFollowStatusService(FollowRepository followRepository,
                                  UserRepository userRepository) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public FollowStatusResponse getFollowStatus(Long targetUserId, Long viewerUserId) {
        userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        boolean isFollowing = viewerUserId != null
                && !viewerUserId.equals(targetUserId)
                && followRepository.existsByFollowerIdAndFollowingId(viewerUserId, targetUserId);

        return FollowStatusResponse.builder()
                .targetUserId(targetUserId)
                .following(isFollowing)
                .build();
    }
}
