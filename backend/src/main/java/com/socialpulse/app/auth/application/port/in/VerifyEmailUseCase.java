package com.socialpulse.app.auth.application.port.in;

import com.socialpulse.app.auth.application.dto.request.EmailVerificationRequest;

@FunctionalInterface
public interface VerifyEmailUseCase {
    void verifyEmail(EmailVerificationRequest request);
}
