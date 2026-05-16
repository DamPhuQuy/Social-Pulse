package com.socialpulse.app.user.application.service;

import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.user.application.dto.request.UserProfileMutationRequest;
import com.socialpulse.app.user.application.dto.response.UserViewProfileResponse;
import com.socialpulse.app.user.application.usecase.CreateUserProfileUseCase;
import com.socialpulse.app.user.domain.model.UserProfile;
import com.socialpulse.app.user.domain.repository.UserProfileRepository;
import com.socialpulse.app.user.domain.repository.UserRepository;

public class CreateUserProfileService implements CreateUserProfileUseCase {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserProfileResponseAssembler userProfileResponseAssembler;

    public CreateUserProfileService(UserRepository userRepository,
                                    UserProfileRepository userProfileRepository,
                                    UserProfileResponseAssembler userProfileResponseAssembler) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userProfileResponseAssembler = userProfileResponseAssembler;
    }

    @Override
    @Transactional
    public UserViewProfileResponse createProfile(Long userId, UserProfileMutationRequest request) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        if (userProfileRepository.findByUserId(userId).isPresent()) {
            throw new AppException(UserCode.USER_PROFILE_ALREADY_EXISTS);
        }

        UserProfile savedProfile = userProfileRepository.save(UserProfile.builder()
                .id(userId)
                .displayName(resolveDisplayName(request.getDisplayName(), user.getUsername()))
                .bio(request.getBio())
                .dob(request.getDob())
                .gender(request.getGender())
                .avatarUrl(request.getAvatarUrl())
                .avatarPublicId(request.getAvatarPublicId())
                .build());

        return userProfileResponseAssembler.assemble(user, savedProfile, userId);
    }

    private String resolveDisplayName(String requestedDisplayName, String fallbackUsername) {
        if (requestedDisplayName == null || requestedDisplayName.isBlank()) {
            return fallbackUsername;
        }

        return requestedDisplayName.trim();
    }
}
