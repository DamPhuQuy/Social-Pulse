package com.socialpulse.app.user.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.AuthCode;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.security.encoder.AppPasswordEncoder;
import com.socialpulse.app.user.application.dto.request.ChangePasswordRequest;
import com.socialpulse.app.user.application.usecase.ChangePasswordUseCase;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.repository.UserRepository;

@Service
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
            throw new AppException(AuthCode.INCORRECT_CURRENT_PASSWORD);
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(AuthCode.PASSWORD_NOT_MATCH);
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new AppException(AuthCode.NEW_PASSWORD_SAME_AS_OLD);
        }

        String newPasswordHash = passwordEncoder.encode(request.getNewPassword());
        user.changePassword(newPasswordHash);
        userRepository.save(user);
    }
}
