package com.socialpulse.app.user.application.usecase;

import com.socialpulse.app.user.application.dto.request.ChangePasswordRequest;

public interface ChangePasswordUseCase {
    void changePassword(Long userId, ChangePasswordRequest request);
}
