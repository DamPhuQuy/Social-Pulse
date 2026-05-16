package com.socialpulse.app.user.application.service;

import java.util.Locale;

import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.user.application.dto.mapper.UserMapper;
import com.socialpulse.app.user.application.dto.request.UserViewProfileRequest;
import com.socialpulse.app.user.application.dto.response.UserViewProfileResponse;
import com.socialpulse.app.user.application.usecase.GetUserProfileUseCase;
import com.socialpulse.app.user.domain.repository.UserProfileRepository;

public class GetUserProfileService implements GetUserProfileUseCase {

    private final UserProfileRepository userProfileRepository;
    private final UserMapper userMapper;

    public GetUserProfileService(
        UserProfileRepository userProfileRepository,
        UserMapper userMapper) {
        this.userProfileRepository = userProfileRepository;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public UserViewProfileResponse getProfile(UserViewProfileRequest request) {
        var userProfile = userProfileRepository.findByUserId(request.getTargetUserId())
            .orElseThrow(() -> new AppException(UserCode.USER_PROFILE_NOT_FOUND));
        return userMapper.toUserViewProfileResponse(userProfile);
    }

    @Override
    @Transactional(readOnly = true)
    public UserViewProfileResponse getProfileByUsername(String username) {
        var normalizedUsername = username == null ? null : username.toLowerCase(Locale.ROOT);
        var userProfile = userProfileRepository.findByUsername(normalizedUsername)
                .orElseThrow(() -> new AppException(UserCode.USER_PROFILE_NOT_FOUND));
        return userMapper.toUserViewProfileResponse(userProfile);
    }
}

