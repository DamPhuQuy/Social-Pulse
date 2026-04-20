package com.socialpulse.app.user.application.service;

import java.util.Locale;

import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.auth.security.encoder.AppPasswordEncoder;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.AuthCode;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.user.application.dto.mapper.UserMapper;
import com.socialpulse.app.user.application.dto.request.UserCreationRequest;
import com.socialpulse.app.user.application.dto.response.UserCreationResponse;
import com.socialpulse.app.user.application.port.in.CreateUserUseCase;
import com.socialpulse.app.user.application.port.out.UserRepositoryPort;
import com.socialpulse.app.user.domain.model.User;

public class CreateUserService implements CreateUserUseCase {

    private final UserRepositoryPort userRepository;
    private final AppPasswordEncoder passwordEncode;
    private final UserMapper userMapper;

    public CreateUserService(UserRepositoryPort userRepository, AppPasswordEncoder passwordEncode, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncode = passwordEncode;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public UserCreationResponse createUser(UserCreationRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(UserCode.USER_ALREADY_EXISTS);
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new AppException(UserCode.USER_ALREADY_EXISTS);
        }

        if (!request.getRawPassword().equals(request.getConfirmPassword())) {
            throw new AppException(AuthCode.PASSWORD_NOT_MATCH);
        }

        String encodedPassword = passwordEncode.encode(request.getRawPassword());
        User user = userMapper.toUser(request, normalizedEmail, encodedPassword);
        user.applyDefaultState();

        user = userRepository.save(user);
        String message = "User created successfully for email: " + user.getEmail();

        return userMapper.toUserCreationResponse(user, message);
    }

}
