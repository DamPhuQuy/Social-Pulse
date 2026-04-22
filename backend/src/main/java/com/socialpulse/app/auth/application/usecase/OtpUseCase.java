package com.socialpulse.app.auth.application.usecase;

public interface OtpUseCase {
    void generateToStoreAndSendEmail(String email);

    void verifyOtp(String email, String otpCode);

    void invalidateOtp(String email);
}

