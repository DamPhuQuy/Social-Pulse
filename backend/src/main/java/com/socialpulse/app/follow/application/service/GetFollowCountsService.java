package com.socialpulse.app.follow.application.service;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.follow.application.dto.response.FollowCountsResponse;
import com.socialpulse.app.follow.application.usecase.GetFollowCountsUseCase;
import com.socialpulse.app.follow.domain.repository.FollowRepository;
import com.socialpulse.app.user.domain.repository.UserRepository;

@Service
public class GetFollowCountsService implements GetFollowCountsUseCase {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    public GetFollowCountsService(FollowRepository followRepository,
                                  UserRepository userRepository) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public FollowCountsResponse getFollowCounts(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        return FollowCountsResponse.builder()
                .userId(userId)
                .followersCount(followRepository.countByFollowingId(userId))
                .followingCount(followRepository.countByFollowerId(userId))
                .build();
    }
}
