package com.socialpulse.app.user.application.usecase;

import com.socialpulse.app.user.application.dto.request.AdminAssignRoleRequest;
import com.socialpulse.app.user.application.dto.response.AdminUserResponse;

public interface AdminAssignUserRoleUseCase {
    AdminUserResponse assignRoles(Long userId, AdminAssignRoleRequest request);
}
