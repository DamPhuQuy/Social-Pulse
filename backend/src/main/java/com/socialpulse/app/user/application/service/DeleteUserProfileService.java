package com.socialpulse.app.user.application.service;

import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.user.application.usecase.DeleteUserProfileUseCase;
import com.socialpulse.app.user.domain.repository.UserProfileRepository;

public class DeleteUserProfileService implements DeleteUserProfileUseCase {

    private final UserProfileRepository userProfileRepository;

    public DeleteUserProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @Override
    @Transactional
    public void deleteProfile(Long userId) {
        if (userProfileRepository.findByUserId(userId).isEmpty()) {
            throw new AppException(UserCode.USER_PROFILE_NOT_FOUND);
        }

        userProfileRepository.deleteByUserId(userId);
    }
}
