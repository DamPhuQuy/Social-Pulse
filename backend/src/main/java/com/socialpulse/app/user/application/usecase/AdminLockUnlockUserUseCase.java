package com.socialpulse.app.user.application.usecase;

import com.socialpulse.app.user.application.dto.response.AdminUserResponse;

public interface AdminLockUnlockUserUseCase {
    AdminUserResponse lockUser(Long userId);
    AdminUserResponse unlockUser(Long userId);
}
