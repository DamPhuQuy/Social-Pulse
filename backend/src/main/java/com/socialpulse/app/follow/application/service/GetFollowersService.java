package com.socialpulse.app.follow.application.service;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.common.dto.response.PageResponse;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.follow.application.usecase.GetFollowersUseCase;
import com.socialpulse.app.follow.domain.repository.FollowRepository;
import com.socialpulse.app.user.application.dto.response.UserSummary;
import com.socialpulse.app.user.domain.repository.UserRepository;

@Service
public class GetFollowersService implements GetFollowersUseCase {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final FollowGraphPageService followGraphPageService;

    public GetFollowersService(FollowRepository followRepository,
                               UserRepository userRepository,
                               FollowGraphPageService followGraphPageService) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
        this.followGraphPageService = followGraphPageService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserSummary> getFollowers(Long userId, int page, int size) {
        ensureUserExists(userId);
        var pageable = PageRequest.of(page, size);
        return followGraphPageService.build(followRepository.findFollowerIdsByFollowingId(userId, pageable));
    }

    private void ensureUserExists(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));
    }
}
