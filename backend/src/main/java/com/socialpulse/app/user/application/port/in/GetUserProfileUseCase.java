package com.socialpulse.app.user.application.port.in;

import com.socialpulse.app.user.application.dto.request.UserViewProfileRequest;
import com.socialpulse.app.user.application.dto.response.UserViewProfileResponse;

public interface GetUserProfileUseCase {
    UserViewProfileResponse getProfile(UserViewProfileRequest request);

    UserViewProfileResponse getProfileByUsername(String username);
}
