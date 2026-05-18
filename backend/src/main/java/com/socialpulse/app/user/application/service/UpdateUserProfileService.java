package com.socialpulse.app.user.application.service;

import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.user.application.dto.request.UserProfileMutationRequest;
import com.socialpulse.app.user.application.dto.response.UserViewProfileResponse;
import com.socialpulse.app.user.application.usecase.UpdateUserProfileUseCase;
import com.socialpulse.app.user.domain.model.UserProfile;
import com.socialpulse.app.user.domain.repository.UserProfileRepository;
import com.socialpulse.app.user.domain.repository.UserRepository;

public class UpdateUserProfileService implements UpdateUserProfileUseCase {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserProfileResponseAssembler userProfileResponseAssembler;

    public UpdateUserProfileService(UserRepository userRepository,
                                    UserProfileRepository userProfileRepository,
                                    UserProfileResponseAssembler userProfileResponseAssembler) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userProfileResponseAssembler = userProfileResponseAssembler;
    }

    @Override
    @Transactional
    public UserViewProfileResponse updateProfile(Long userId, UserProfileMutationRequest request) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        // Change username if requested
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new AppException(UserCode.USERNAME_ALREADY_TAKEN);
            }
            user.changeUsername(request.getUsername());
            userRepository.save(user);
        }

        UserProfile existingProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(UserCode.USER_PROFILE_NOT_FOUND));

        UserProfile savedProfile = userProfileRepository.save(UserProfile.builder()
                .id(existingProfile.getId())
                .displayName(resolveDisplayName(request.getDisplayName(), existingProfile.getDisplayName(), user.getUsername()))
                .bio(request.getBio() != null ? request.getBio() : existingProfile.getBio())
                .dob(request.getDob() != null ? request.getDob() : existingProfile.getDob())
                .gender(request.getGender() != null ? request.getGender() : existingProfile.getGender())
                .avatarUrl(request.getAvatarUrl() != null ? request.getAvatarUrl() : existingProfile.getAvatarUrl())
                .avatarPublicId(request.getAvatarPublicId() != null ? request.getAvatarPublicId() : existingProfile.getAvatarPublicId())
                .updatedAt(existingProfile.getUpdatedAt())
                .build());

        return userProfileResponseAssembler.assemble(user, savedProfile, userId);
    }

    private String resolveDisplayName(String requestedDisplayName, String currentDisplayName, String fallbackUsername) {
        if (requestedDisplayName != null) {
            if (requestedDisplayName.isBlank()) {
                return fallbackUsername;
            }
            return requestedDisplayName.trim();
        }

        if (currentDisplayName == null || currentDisplayName.isBlank()) {
            return fallbackUsername;
        }

        return currentDisplayName;
    }
}
