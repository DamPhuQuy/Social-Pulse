package com.socialpulse.app.user.application.service;

import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.user.application.dto.mapper.UserMapper;
import com.socialpulse.app.user.application.dto.response.AdminUserResponse;
import com.socialpulse.app.user.application.usecase.AdminLockUnlockUserUseCase;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.repository.UserRepository;

public class AdminLockUnlockUserService implements AdminLockUnlockUserUseCase {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public AdminLockUnlockUserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public AdminUserResponse lockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        user.lockAccount();
        User savedUser = userRepository.save(user);
        return userMapper.toAdminUserResponse(savedUser);
    }

    @Override
    @Transactional
    public AdminUserResponse unlockUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        user.activeAccount();
        user.resetFailedLoginAttempts();
        User savedUser = userRepository.save(user);
        return userMapper.toAdminUserResponse(savedUser);
    }
}
