package com.socialpulse.app.user.application.usecase;

import com.socialpulse.app.user.application.dto.response.AdminUserResponse;

public interface AdminGetUserDetailsUseCase {
    AdminUserResponse getUserDetails(Long userId);
}
