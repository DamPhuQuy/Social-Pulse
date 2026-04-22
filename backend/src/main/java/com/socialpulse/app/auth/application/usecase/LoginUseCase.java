package com.socialpulse.app.auth.application.usecase;

import com.socialpulse.app.auth.application.dto.TokenPair;
import com.socialpulse.app.auth.application.dto.request.LoginRequest;

@FunctionalInterface
public interface LoginUseCase {
    TokenPair login(LoginRequest request);
}

