package com.socialpulse.app.follow.application.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.follow.application.dto.response.FollowingListResponse;
import com.socialpulse.app.follow.application.dto.response.FollowingResponse;
import com.socialpulse.app.follow.application.usecase.GetFollowingUseCase;
import com.socialpulse.app.follow.domain.model.Follow;
import com.socialpulse.app.follow.domain.repository.FollowRepository;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.repository.UserRepository;

@Service
public class GetFollowingService implements GetFollowingUseCase {
    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    public GetFollowingService(FollowRepository followRepository, UserRepository userRepository) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
    }

    @Override
    public FollowingListResponse getFollowing(Long userId, Long currentUserId, int page, int size) {
        // Verify user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        int offset = page * size;

        // Get following
        List<Follow> follows = followRepository.findFollowingByUserId(userId, offset, size);

        // Get following user IDs
        Set<Long> followingIds = follows.stream()
                .map(Follow::getFollowingId)
                .collect(Collectors.toSet());

        // Get user details
        List<User> users = userRepository.findByIds(followingIds);

        // Check if these users are following current user back
        Set<Long> followingBackIds = followRepository.findFollowedUserIds(currentUserId, followingIds);

        // Build response
        List<FollowingResponse> followingResponses = users.stream()
                .map(user -> FollowingResponse.builder()
                        .userId(user.getId())
                        .username(user.getUsername())
                        .fullName(user.getProfile().getDisplayName())
                        .avatarUrl(user.getProfile().getAvatarUrl())
                        .isFollowingBack(followingBackIds.contains(user.getId()))
                        .build())
                .collect(Collectors.toList());

        long totalCount = followRepository.countByFollowerId(userId);
        boolean hasNext = (offset + size) < totalCount;

        return FollowingListResponse.builder()
                .following(followingResponses)
                .totalCount(totalCount)
                .page(page)
                .size(size)
                .hasNext(hasNext)
                .build();
    }
}
