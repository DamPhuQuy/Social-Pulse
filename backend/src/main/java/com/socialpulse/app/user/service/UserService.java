package com.socialpulse.app.user.service;

import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.auth.security.encoder.PasswordEncoder;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.status.ErrorCode;
import com.socialpulse.app.user.dto.request.UserCreationRequest;
import com.socialpulse.app.user.dto.response.UserCreationResponse;
import com.socialpulse.app.user.dto.response.UserViewProfileResponse;
import com.socialpulse.app.user.entity.User;
import com.socialpulse.app.user.entity.UserProfile;
import com.socialpulse.app.user.entity.UserRole;
import com.socialpulse.app.user.entity.VerificationStatus;
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

        User user = User.builder()
                .username(userCreation.getUsername())
                .email(normalizedEmail)
                .passwordHash(encoder.encode(userCreation.getRawPassword()))
                .role(UserRole.USER)                        // FIX: set default role
                .verification(VerificationStatus.NOT_VERIFIED)  // FIX: chưa verify email
                .build();

        user = userRepository.save(user);

        return UserCreationResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .message("User created successfully for email: " + user.getEmail())
                .build();
    }

    @Transactional
    public UserViewProfileResponse getProfile(Long userId) {
        UserProfile userProfile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return UserViewProfileResponse.builder()
                .userId(userProfile.getUser().getId())
                .displayName(userProfile.getDisplayName())
                .bio(userProfile.getBio())
                .dob(userProfile.getDob())
                .build();
    }

    @Transactional(readOnly = true)
    public UserViewProfileResponse getProfileByUsername(String username) {
        User user = userRepository.findProfileByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        UserProfile userProfile = user.getProfile();

        return UserViewProfileResponse.builder()
                .userId(user.getId())
                .displayName(userProfile.getDisplayName())
                .bio(userProfile.getBio())
                .dob(userProfile.getDob())
                .build();
    }
}
