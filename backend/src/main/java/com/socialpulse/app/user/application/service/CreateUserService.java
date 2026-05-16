package com.socialpulse.app.user.application.service;

import java.util.Locale;

import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.AuthCode;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.security.encoder.AppPasswordEncoder;
import com.socialpulse.app.user.application.dto.mapper.UserMapper;
import com.socialpulse.app.user.application.dto.request.UserCreationRequest;
import com.socialpulse.app.user.application.dto.response.UserCreationResponse;
import com.socialpulse.app.user.application.usecase.CreateUserUseCase;
import com.socialpulse.app.user.domain.model.UserProfile;
import com.socialpulse.app.user.domain.repository.UserProfileRepository;
import com.socialpulse.app.user.domain.repository.UserRepository;
import com.socialpulse.app.user.domain.model.User;

public class CreateUserService implements CreateUserUseCase {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final AppPasswordEncoder passwordEncode;
    private final UserMapper userMapper;
    private final UserRoleService userRoleService;

    public CreateUserService(UserRepository userRepository,
                             UserProfileRepository userProfileRepository,
                             AppPasswordEncoder passwordEncode,
                             UserMapper userMapper,
                             UserRoleService userRoleService) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordEncode = passwordEncode;
        this.userMapper = userMapper;
        this.userRoleService = userRoleService;
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

        userRoleService.assignDefaultRole(user);

        user = userRepository.save(user);
        userProfileRepository.save(UserProfile.builder()
                .id(user.getId())
                .displayName(user.getUsername())
                .build());
        String message = "User created successfully for email: " + user.getEmail();

        return userMapper.toUserCreationResponse(user, message);
    }

}

