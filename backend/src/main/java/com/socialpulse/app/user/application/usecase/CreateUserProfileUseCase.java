package com.socialpulse.app.user.application.usecase;

import com.socialpulse.app.user.application.dto.request.UserProfileMutationRequest;
import com.socialpulse.app.user.application.dto.response.UserViewProfileResponse;

@FunctionalInterface
public interface CreateUserProfileUseCase {
    UserViewProfileResponse createProfile(Long userId, UserProfileMutationRequest request);
}
