package com.socialpulse.app.auth.application.port.in;

import com.socialpulse.app.user.application.dto.request.UserCreationRequest;
import com.socialpulse.app.user.application.dto.response.UserCreationResponse;

@FunctionalInterface
public interface RegisterUseCase {
    UserCreationResponse register(UserCreationRequest request);
}
