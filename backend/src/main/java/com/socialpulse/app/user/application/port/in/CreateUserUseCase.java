package com.socialpulse.app.user.application.port.in;

import com.socialpulse.app.user.application.dto.request.UserCreationRequest;
import com.socialpulse.app.user.application.dto.response.UserCreationResponse;

@FunctionalInterface
public interface CreateUserUseCase {
    UserCreationResponse createUser(UserCreationRequest request);
}
