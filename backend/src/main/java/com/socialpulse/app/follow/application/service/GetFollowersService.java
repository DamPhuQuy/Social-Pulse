package com.socialpulse.app.follow.application.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.follow.application.dto.response.FollowerResponse;
import com.socialpulse.app.follow.application.dto.response.FollowersListResponse;
import com.socialpulse.app.follow.application.usecase.GetFollowersUseCase;
import com.socialpulse.app.follow.domain.model.Follow;
import com.socialpulse.app.follow.domain.repository.FollowRepository;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.repository.UserRepository;

@Service
public class GetFollowersService implements GetFollowersUseCase {
    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    public GetFollowersService(FollowRepository followRepository, UserRepository userRepository) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
    }

    @Override
    public FollowersListResponse getFollowers(Long userId, Long currentUserId, int page, int size) {
        // Verify user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        int offset = page * size;

        // Get followers
        List<Follow> follows = followRepository.findFollowersByUserId(userId, offset, size);

        // Get follower user IDs
        Set<Long> followerIds = follows.stream()
                .map(Follow::getFollowerId)
                .collect(Collectors.toSet());

        // Get user details
        List<User> users = userRepository.findByIds(followerIds);

        // Check if current user is following these users back
        Set<Long> currentUserFollowingIds = followRepository.findFollowedUserIds(currentUserId, followerIds);

        // Build response
        List<FollowerResponse> followerResponses = users.stream()
                .map(user -> FollowerResponse.builder()
                        .userId(user.getId())
                        .username(user.getUsername())
                        .fullName(user.getProfile().getDisplayName())
                        .avatarUrl(user.getProfile().getAvatarUrl())
                        .isFollowing(currentUserFollowingIds.contains(user.getId()))
                        .build())
                .collect(Collectors.toList());

        long totalCount = followRepository.countByFollowingId(userId);
        boolean hasNext = (offset + size) < totalCount;

        return FollowersListResponse.builder()
                .followers(followerResponses)
                .totalCount(totalCount)
                .page(page)
                .size(size)
                .hasNext(hasNext)
                .build();
    }
}
