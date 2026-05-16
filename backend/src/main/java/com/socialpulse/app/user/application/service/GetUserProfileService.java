package com.socialpulse.app.user.application.service;

import java.util.Locale;

import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.user.application.dto.request.UserViewProfileRequest;
import com.socialpulse.app.user.application.dto.response.UserViewProfileResponse;
import com.socialpulse.app.user.application.usecase.GetUserProfileUseCase;
import com.socialpulse.app.user.domain.repository.UserRepository;
import com.socialpulse.app.user.domain.repository.UserProfileRepository;

public class GetUserProfileService implements GetUserProfileUseCase {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserProfileResponseAssembler userProfileResponseAssembler;

    public GetUserProfileService(
        UserRepository userRepository,
        UserProfileRepository userProfileRepository,
        UserProfileResponseAssembler userProfileResponseAssembler) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userProfileResponseAssembler = userProfileResponseAssembler;
    }

    @Override
    @Transactional(readOnly = true)
    public UserViewProfileResponse getProfile(UserViewProfileRequest request, Long viewerUserId) {
        var user = userRepository.findById(request.getTargetUserId())
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));
        var userProfile = userProfileRepository.findByUserId(request.getTargetUserId())
                .orElseThrow(() -> new AppException(UserCode.USER_PROFILE_NOT_FOUND));
        return userProfileResponseAssembler.assemble(user, userProfile, viewerUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserViewProfileResponse getProfileByUsername(String username, Long viewerUserId) {
        var normalizedUsername = username == null ? null : username.toLowerCase(Locale.ROOT);
        var user = userRepository.findByUsername(normalizedUsername)
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));
        var userProfile = userProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AppException(UserCode.USER_PROFILE_NOT_FOUND));
        return userProfileResponseAssembler.assemble(user, userProfile, viewerUserId);
    }
}
