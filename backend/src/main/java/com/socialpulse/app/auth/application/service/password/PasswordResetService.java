package com.socialpulse.app.auth.application.service.password;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.auth.application.dto.request.ForgotPasswordRequest;
import com.socialpulse.app.auth.application.dto.request.ResendOtpRequest;
import com.socialpulse.app.auth.application.dto.request.ResetPasswordRequest;
import com.socialpulse.app.auth.application.dto.request.VerifyOtpRequest;
import com.socialpulse.app.auth.application.usecase.OtpUseCase;
import com.socialpulse.app.auth.application.usecase.PasswordResetUseCase;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.security.encoder.AppPasswordEncoder;
import com.socialpulse.app.user.domain.repository.UserRepository;
import com.socialpulse.app.user.domain.model.User;

@Service
public class PasswordResetService implements PasswordResetUseCase {

    private final UserRepository userRepository;
    private final AppPasswordEncoder passwordEncoder;
    private final OtpUseCase otpUseCase;

    public PasswordResetService(UserRepository userRepository,
                                AppPasswordEncoder passwordEncoder,
                                OtpUseCase otpUseCase) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpUseCase = otpUseCase;
    }

    @Override
    @Transactional
    public void processForgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail();

        userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        otpUseCase.generateToStoreAndSendEmail(email);
    }

    @Override
    @Transactional
    public void processResendOtp(ResendOtpRequest request) {
        String email = request.getEmail();

        userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        otpUseCase.generateToStoreAndSendEmail(email);
    }

    @Override
    @Transactional
    public void processVerifyOtp(VerifyOtpRequest request) {
        String email = request.getEmail();

        userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        otpUseCase.verifyOtp(email, request.getOtpCode());
    }

    @Override
    @Transactional
    public void processResetPassword(ResetPasswordRequest request) {
        String email = request.getEmail();
        String otpCode = request.getOtpCode();
        String newPassword = request.getNewPassword();

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        otpUseCase.verifyOtp(email, otpCode);

        user.changePassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        otpUseCase.invalidateOtp(email);
    }
}


