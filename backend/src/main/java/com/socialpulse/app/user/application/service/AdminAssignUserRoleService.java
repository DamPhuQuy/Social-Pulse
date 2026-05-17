package com.socialpulse.app.user.application.service;

import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.user.application.dto.mapper.UserMapper;
import com.socialpulse.app.user.application.dto.request.AdminAssignRoleRequest;
import com.socialpulse.app.user.application.dto.response.AdminUserResponse;
import com.socialpulse.app.user.application.usecase.AdminAssignUserRoleUseCase;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.repository.UserRepository;

public class AdminAssignUserRoleService implements AdminAssignUserRoleUseCase {

    private final UserRepository userRepository;
    private final UserRoleService userRoleService;
    private final UserMapper userMapper;

    public AdminAssignUserRoleService(UserRepository userRepository,
                                      UserRoleService userRoleService,
                                      UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userRoleService = userRoleService;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public AdminUserResponse assignRoles(Long userId, AdminAssignRoleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        userRoleService.assignRoles(user, request.getRoles());
        User savedUser = userRepository.save(user);
        return userMapper.toAdminUserResponse(savedUser);
    }
}
