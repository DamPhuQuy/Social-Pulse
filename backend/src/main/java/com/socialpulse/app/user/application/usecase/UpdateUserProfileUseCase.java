package com.socialpulse.app.user.application.usecase;

import com.socialpulse.app.user.application.dto.request.UserProfileMutationRequest;
import com.socialpulse.app.user.application.dto.response.UserViewProfileResponse;

@FunctionalInterface
public interface UpdateUserProfileUseCase {
    UserViewProfileResponse updateProfile(Long userId, UserProfileMutationRequest request);
}
