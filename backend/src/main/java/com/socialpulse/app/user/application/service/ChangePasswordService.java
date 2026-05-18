package com.socialpulse.app.user.application.service;

import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.security.encoder.AppPasswordEncoder;
import com.socialpulse.app.user.application.dto.request.ChangePasswordRequest;
import com.socialpulse.app.user.application.usecase.ChangePasswordUseCase;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.repository.UserRepository;

public class ChangePasswordService implements ChangePasswordUseCase {
    private final UserRepository userRepository;
    private final AppPasswordEncoder passwordEncoder;

    public ChangePasswordService(UserRepository userRepository, AppPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new AppException(UserCode.INVALID_PASSWORD);
        }

        user.changePassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
