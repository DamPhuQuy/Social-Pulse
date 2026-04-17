package com.socialpulse.app.auth.service.password;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.auth.dto.request.ForgotPasswordRequest;
import com.socialpulse.app.auth.dto.request.ResendOtpRequest;
import com.socialpulse.app.auth.dto.request.ResetPasswordRequest;
import com.socialpulse.app.auth.security.encoder.AppPasswordEncoder;
import com.socialpulse.app.auth.service.otp.OtpService;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.status.UserCode;
import com.socialpulse.app.user.entity.User;
import com.socialpulse.app.user.repository.UserRepository;

@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final AppPasswordEncoder passwordEncoder;
    private final OtpService otpService;

    public PasswordResetService(UserRepository userRepository,
                                AppPasswordEncoder passwordEncoder,
                                OtpService otpService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
    }

    @Transactional
    public void processForgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail();

        userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        otpService.generateToStoreAndSendEmail(email);
    }

    @Transactional
    public void processResendOtp(ResendOtpRequest request) {
        String email = request.getEmail();

        userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        otpService.generateToStoreAndSendEmail(email);
    }

    @Transactional
    public void processResetPassword(ResetPasswordRequest request) {
        String email = request.getEmail();
        String newPassword = request.getNewPassword();

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        otpService.invalidateOtp(email);
    }
}
