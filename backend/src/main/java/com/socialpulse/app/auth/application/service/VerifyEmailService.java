package com.socialpulse.app.auth.application.service;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.auth.application.dto.request.EmailVerificationRequest;
import com.socialpulse.app.auth.application.usecase.OtpUseCase;
import com.socialpulse.app.auth.application.usecase.VerifyEmailUseCase;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.user.domain.repository.UserRepository;
import com.socialpulse.app.user.domain.enums.UserStatus;

public class VerifyEmailService implements VerifyEmailUseCase {

    private final UserRepository userRepository;
    private final OtpUseCase otpUseCase;
    private final Logger logger;

    public VerifyEmailService(UserRepository userRepository, OtpUseCase otpUseCase) {
        this.userRepository = userRepository;
        this.otpUseCase = otpUseCase;
        this.logger = LoggerFactory.getLogger(VerifyEmailService.class);
    }

    @Override
    @Transactional
    public void verifyEmail(EmailVerificationRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
        logger.info("Attempting to verify email: {}", normalizedEmail);

        var user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.ACTIVE) {
            otpUseCase.invalidateOtp(normalizedEmail);
            logger.info("Email already verified for: {}", normalizedEmail);
            return;
        }

        otpUseCase.verifyOtp(normalizedEmail, request.getOtpCode());
        user.activeAccount();
        user.verifyAccount();
        userRepository.save(user);
        otpUseCase.invalidateOtp(normalizedEmail);
        logger.info("Email verified for: {}", normalizedEmail);
    }
}


