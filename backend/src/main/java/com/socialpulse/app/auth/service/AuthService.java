package com.socialpulse.app.auth.service;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.auth.dto.request.EmailVerificationRequest;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.ErrorCode;
import com.socialpulse.app.user.dto.request.UserCreationRequest;
import com.socialpulse.app.user.dto.response.UserCreationResponse;
import com.socialpulse.app.user.entity.User;
import com.socialpulse.app.user.entity.UserStatus;
import com.socialpulse.app.user.repository.UserRepository;
import com.socialpulse.app.user.service.UserService;

@Service
public class AuthService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final OtpService otpService;
    private final Logger logger;

    public AuthService(UserService userService, UserRepository userRepository, OtpService otpService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.otpService = otpService;
        this.logger = LoggerFactory.getLogger(AuthService.class);
    }

    @Transactional
    public UserCreationResponse register(UserCreationRequest request) {
        UserCreationResponse response = userService.createUser(request);

        logger.info("User registered with email: {}", request.getEmail());

        otpService.generateAndStoreOtp(request.getEmail());

        logger.info("OTP generated and stored for email: {}", request.getEmail());

        return response;
    }

    @Transactional
    public void verifyEmail(EmailVerificationRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);

        logger.info("Attempting to verify email: {}", normalizedEmail);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.ACTIVE) {
            otpService.invalidateOtp(normalizedEmail);
            logger.info("Email already verified for: {}", normalizedEmail);

            return;
        }

        otpService.verifyOtp(normalizedEmail, request.getOtpCode());
        user.activeAccount();
        userRepository.save(user);
        otpService.invalidateOtp(normalizedEmail);

        logger.info("Email verified for: {}", normalizedEmail);
    }
}
