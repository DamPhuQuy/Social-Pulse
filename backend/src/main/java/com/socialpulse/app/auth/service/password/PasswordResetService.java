package com.socialpulse.app.auth.service.password;

import com.socialpulse.app.auth.dto.request.ForgotPasswordRequest;
import com.socialpulse.app.auth.dto.request.ResendOtpRequest;
import com.socialpulse.app.auth.dto.request.ResetPasswordRequest;
import com.socialpulse.app.auth.service.otp.OtpService;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.status.ErrorCode;
import com.socialpulse.app.user.entity.User;
import com.socialpulse.app.user.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    public PasswordResetService(UserRepository userRepository, 
                                PasswordEncoder passwordEncoder, 
                                OtpService otpService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
    }

    @Transactional
    public void processForgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail();
        
        userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        otpService.generateToStoreAndSendEmail(email);
    }

    @Transactional
    public void processResendOtp(ResendOtpRequest request) {
        String email = request.getEmail();

        userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        otpService.generateToStoreAndSendEmail(email);
    }

    @Transactional
    public void processResetPassword(ResetPasswordRequest request) {
        String email = request.getEmail();
        String otpInput = request.getOtp();
        String newPassword = request.getNewPassword();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        otpService.verifyOtp(email, otpInput);

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        otpService.invalidateOtp(email);
    }
}