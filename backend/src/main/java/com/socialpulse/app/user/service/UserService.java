package com.socialpulse.app.user.service;

import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.ErrorCode;
import com.socialpulse.app.common.security.PasswordEncoder;
import com.socialpulse.app.user.dto.request.UserCreationRequest;
import com.socialpulse.app.user.dto.response.UserCreationResponse;
import com.socialpulse.app.user.entity.User;
import com.socialpulse.app.user.repository.UserProfileRepository;
import com.socialpulse.app.user.repository.UserRepository;

import jakarta.validation.Valid;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder encoder;

    public UserService(UserRepository userRepository, UserProfileRepository userProfileRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.encoder = passwordEncoder;
    }

    @Transactional
    public UserCreationResponse createUser(@Valid UserCreationRequest userCreation) {
        String normalizedEmail = userCreation.getEmail().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByUsername(userCreation.getUsername())) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }

        if (!userCreation.getRawPassword().equals(userCreation.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_NOT_MATCH);
        }

        User user = User.builder().username(userCreation.getUsername())
                .email(normalizedEmail)
                .passwordHash(encoder.encode(userCreation.getRawPassword()))
                .build();
        user = userRepository.save(user);

        return UserCreationResponse.builder().id(user.getId()).username(user.getUsername()).message("User created successfully").build();
    }
}
