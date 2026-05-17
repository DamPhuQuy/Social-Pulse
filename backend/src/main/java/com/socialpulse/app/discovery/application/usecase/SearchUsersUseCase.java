package com.socialpulse.app.discovery.application.usecase;

import com.socialpulse.app.common.dto.response.PageResponse;
import com.socialpulse.app.discovery.application.dto.response.SearchUserResponse;

public interface SearchUsersUseCase {
    PageResponse<SearchUserResponse> searchUsers(String query, int page, int size);
}
