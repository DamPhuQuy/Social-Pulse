package com.socialpulse.app.auth.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.auth.application.usecase.OtpUseCase;
import com.socialpulse.app.auth.application.usecase.RegisterUseCase;
import com.socialpulse.app.user.application.dto.request.UserCreationRequest;
import com.socialpulse.app.user.application.dto.response.UserCreationResponse;
import com.socialpulse.app.user.application.usecase.CreateUserUseCase;

public class RegisterService implements RegisterUseCase {

    private final CreateUserUseCase createUserUseCase;
    private final OtpUseCase otpUseCase;
    private final Logger logger;

    public RegisterService(CreateUserUseCase createUserUseCase, OtpUseCase otpUseCase) {
        this.createUserUseCase = createUserUseCase;
        this.otpUseCase = otpUseCase;
        this.logger = LoggerFactory.getLogger(RegisterService.class);
    }

    @Override
    @Transactional
    public UserCreationResponse register(UserCreationRequest request) {
        UserCreationResponse response = createUserUseCase.createUser(request);
        logger.info("User registered with email: {}", request.getEmail());
        otpUseCase.generateToStoreAndSendEmail(request.getEmail());
        return response;
    }
}

