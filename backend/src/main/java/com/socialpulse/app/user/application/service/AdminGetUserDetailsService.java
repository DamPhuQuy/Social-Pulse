package com.socialpulse.app.user.application.service;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.user.application.dto.mapper.UserMapper;
import com.socialpulse.app.user.application.dto.response.AdminUserResponse;
import com.socialpulse.app.user.application.usecase.AdminGetUserDetailsUseCase;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.repository.UserRepository;

public class AdminGetUserDetailsService implements AdminGetUserDetailsUseCase {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public AdminGetUserDetailsService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    public AdminUserResponse getUserDetails(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));
        return userMapper.toAdminUserResponse(user);
    }
}
