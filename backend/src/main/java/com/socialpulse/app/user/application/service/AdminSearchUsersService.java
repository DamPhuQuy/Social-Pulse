package com.socialpulse.app.user.application.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.socialpulse.app.common.dto.response.PageResponse;
import com.socialpulse.app.user.application.dto.mapper.UserMapper;
import com.socialpulse.app.user.application.dto.response.AdminUserResponse;
import com.socialpulse.app.user.application.usecase.AdminSearchUsersUseCase;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.repository.UserRepository;

public class AdminSearchUsersService implements AdminSearchUsersUseCase {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public AdminSearchUsersService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    public PageResponse<AdminUserResponse> searchUsers(String query, int page, int size) {
        String searchQuery = (query == null) ? "" : query.trim();
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = userRepository.searchByQuery(searchQuery, pageable);

        List<AdminUserResponse> items = userPage.getContent().stream()
                .map(userMapper::toAdminUserResponse)
                .toList();

        return PageResponse.<AdminUserResponse>builder()
                .items(items)
                .page(userPage.getNumber())
                .size(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .hasNext(userPage.hasNext())
                .build();
    }
}
