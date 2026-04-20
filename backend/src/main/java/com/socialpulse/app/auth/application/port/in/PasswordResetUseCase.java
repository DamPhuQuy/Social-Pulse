package com.socialpulse.app.auth.application.port.in;

import com.socialpulse.app.auth.application.dto.request.ForgotPasswordRequest;
import com.socialpulse.app.auth.application.dto.request.ResendOtpRequest;
import com.socialpulse.app.auth.application.dto.request.ResetPasswordRequest;
import com.socialpulse.app.auth.application.dto.request.VerifyOtpRequest;

public interface PasswordResetUseCase {
    void processForgotPassword(ForgotPasswordRequest request);

    void processResendOtp(ResendOtpRequest request);

    void processVerifyOtp(VerifyOtpRequest request);

    void processResetPassword(ResetPasswordRequest request);
}
