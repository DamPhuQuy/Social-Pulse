package com.socialpulse.app.user.service;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.ErrorCode;
import com.socialpulse.app.common.security.PasswordEncoder;
import com.socialpulse.app.user.dto.request.UserCreation;
import com.socialpulse.app.user.entity.User;
import com.socialpulse.app.user.repository.UserProfileRepository;
import com.socialpulse.app.user.repository.UserRepository;

import jakarta.validation.Valid;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    public UserService(UserRepository userRepository, UserProfileRepository userProfileRepository) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
    }

    public void createUser(@Valid @RequestBody UserCreation userCreation) {
        if (userRepository.existsByUsername(userCreation.getUsername())) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }

        if (!userCreation.getRawPassword().equals(userCreation.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_NOT_MATCH);
        }

        User user = User.builder().username(userCreation.getUsername())
                .email(userCreation.getEmail())
                .passwordHash(PasswordEncoder.encode(userCreation.getRawPassword()))
                .build();
        userRepository.save(user);
    }
}
