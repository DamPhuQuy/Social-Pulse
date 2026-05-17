package com.socialpulse.app.user.application.usecase;

import com.socialpulse.app.common.dto.response.PageResponse;
import com.socialpulse.app.user.application.dto.response.AdminUserResponse;

public interface AdminSearchUsersUseCase {
    PageResponse<AdminUserResponse> searchUsers(String query, int page, int size);
}
