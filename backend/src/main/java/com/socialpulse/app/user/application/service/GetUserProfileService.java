package com.socialpulse.app.user.application.service;

import java.util.Locale;

import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.user.application.dto.mapper.UserProfileMapper;
import com.socialpulse.app.user.application.dto.request.UserViewProfileRequest;
import com.socialpulse.app.user.application.dto.response.UserViewProfileResponse;
import com.socialpulse.app.user.application.port.in.GetUserProfileUseCase;
import com.socialpulse.app.user.application.port.out.UserProfileRepositoryPort;

public class GetUserProfileService implements GetUserProfileUseCase {

    private final UserProfileRepositoryPort userProfileRepository;
    private final UserProfileMapper userProfileMapper;

    public GetUserProfileService(
        UserProfileRepositoryPort userProfileRepository,
        UserProfileMapper userProfileMapper) {
        this.userProfileRepository = userProfileRepository;
        this.userProfileMapper = userProfileMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public UserViewProfileResponse getProfile(UserViewProfileRequest request) {
        var userProfile = userProfileRepository.findByUserId(request.getTargetUserId())
            .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));
        return userProfileMapper.toUserViewProfileResponse(userProfile);
    }

    @Override
    @Transactional(readOnly = true)
    public UserViewProfileResponse getProfileByUsername(String username) {
        var normalizedUsername = username == null ? null : username.toLowerCase(Locale.ROOT);
        var userProfile = userProfileRepository.findByUsername(normalizedUsername)
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));
        return userProfileMapper.toUserViewProfileResponse(userProfile);
    }
}
