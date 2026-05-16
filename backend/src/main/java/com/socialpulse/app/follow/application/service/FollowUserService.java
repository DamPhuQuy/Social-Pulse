package com.socialpulse.app.follow.application.service;

import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.follow.application.dto.mapper.FollowMapper;
import com.socialpulse.app.follow.application.dto.response.FollowResponse;
import com.socialpulse.app.follow.application.usecase.FollowUserUseCase;
import com.socialpulse.app.follow.domain.model.Follow;
import com.socialpulse.app.follow.domain.repository.FollowRepository;
import com.socialpulse.app.notification.application.service.NotificationCommandService;
import com.socialpulse.app.security.user.CustomUserDetails;
import com.socialpulse.app.user.domain.repository.UserRepository;

public class FollowUserService implements FollowUserUseCase {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final FollowMapper followMapper;
    private final NotificationCommandService notificationCommandService;

    public FollowUserService(FollowRepository followRepository,
                             UserRepository userRepository,
                             FollowMapper followMapper,
                             NotificationCommandService notificationCommandService) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
        this.followMapper = followMapper;
        this.notificationCommandService = notificationCommandService;
    }

    @Override
    @Transactional
    public FollowResponse followUser(Long followingId, CustomUserDetails currentUser) {
        if (currentUser.getId().equals(followingId)) {
            throw new AppException(UserCode.CANNOT_FOLLOW_YOURSELF);
        }

        userRepository.findById(followingId)
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        if (followRepository.existsByFollowerIdAndFollowingId(currentUser.getId(), followingId)) {
            throw new AppException(UserCode.ALREADY_FOLLOWING);
        }

        Follow follow = Follow.builder()
                .followerId(currentUser.getId())
                .followingId(followingId)
                .build();

        Follow savedFollow = followRepository.save(follow);
        notificationCommandService.notifyFollow(currentUser.getId(), followingId);
        return followMapper.toFollowResponse(savedFollow);
    }
}
