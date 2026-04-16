package com.socialpulse.app.user.service;

import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.auth.security.encoder.PasswordEncoder;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.status.AuthCode;
import com.socialpulse.app.common.status.UserCode;
import com.socialpulse.app.user.dto.request.UserCreationRequest;
import com.socialpulse.app.user.dto.response.UserCreationResponse;
import com.socialpulse.app.user.dto.response.UserViewProfileResponse;
import com.socialpulse.app.user.entity.User;
import com.socialpulse.app.user.entity.UserProfile;
import com.socialpulse.app.user.mapper.UserMapper;
import com.socialpulse.app.user.mapper.UserProfileMapper;
import com.socialpulse.app.user.repository.UserProfileRepository;
import com.socialpulse.app.user.repository.UserRepository;

import jakarta.validation.Valid;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder encoder;
    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;

    public UserService(UserRepository userRepository,
                       UserProfileRepository userProfileRepository,
                       PasswordEncoder passwordEncoder,
                       UserMapper userMapper,
                       UserProfileMapper userProfileMapper) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.encoder = passwordEncoder;
        this.userMapper = userMapper;
        this.userProfileMapper = userProfileMapper;
    }

    @Transactional
    public UserCreationResponse createUser(@Valid UserCreationRequest userCreation) {
        String normalizedEmail = userCreation.getEmail().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByUsername(userCreation.getUsername())) {
            throw new AppException(UserCode.USER_ALREADY_EXISTS);
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new AppException(UserCode.USER_ALREADY_EXISTS);
        }

        if (!userCreation.getRawPassword().equals(userCreation.getConfirmPassword())) {
            throw new AppException(AuthCode.PASSWORD_NOT_MATCH);
        }

        String encodedPassword = encoder.encode(userCreation.getRawPassword());
        User user = userMapper.toUser(userCreation, normalizedEmail, encodedPassword);

        user = userRepository.save(user);
        String message = "User created successfully for email: " + user.getEmail();

        return userMapper.toUserCreationResponse(user, message);
    }

    @Transactional
    public UserViewProfileResponse getProfile(Long userId) {
        UserProfile userProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        return userProfileMapper.toUserViewProfileResponse(userProfile);
    }

    @Transactional(readOnly = true)
    public UserViewProfileResponse getProfileByUsername(String username) {
        User user = userRepository.findProfileByUsername(username)
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        return userProfileMapper.toUserViewProfileResponse(user);
    }
}
