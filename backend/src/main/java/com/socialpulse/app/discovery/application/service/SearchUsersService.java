package com.socialpulse.app.discovery.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.socialpulse.app.common.dto.response.PageResponse;
import com.socialpulse.app.discovery.application.dto.response.SearchUserResponse;
import com.socialpulse.app.discovery.application.usecase.SearchUsersUseCase;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.repository.UserRepository;

public class SearchUsersService implements SearchUsersUseCase {
    private final UserRepository userRepository;

    public SearchUsersService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public PageResponse<SearchUserResponse> searchUsers(String query, int page, int size) {
        if (query == null || query.isBlank()) {
            return emptyPage(page, size);
        }

        Page<User> users = userRepository.searchByQuery(
                query.trim(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "username")));

        return PageResponse.<SearchUserResponse>builder()
                .items(users.getContent().stream().map(this::toResponse).toList())
                .page(users.getNumber())
                .size(users.getSize())
                .totalElements(users.getTotalElements())
                .totalPages(users.getTotalPages())
                .hasNext(users.hasNext())
                .build();
    }

    private SearchUserResponse toResponse(User user) {
        return SearchUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(user.getProfile() != null ? user.getProfile().getDisplayName() : null)
                .avatarUrl(user.getProfile() != null ? user.getProfile().getAvatarUrl() : null)
                .build();
    }

    private PageResponse<SearchUserResponse> emptyPage(int page, int size) {
        return PageResponse.<SearchUserResponse>builder()
                .items(java.util.List.of())
                .page(page)
                .size(size)
                .totalElements(0)
                .totalPages(0)
                .hasNext(false)
                .build();
    }
}
