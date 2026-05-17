package com.socialpulse.app.user.application.usecase;

@FunctionalInterface
public interface DeleteUserProfileUseCase {
    void deleteProfile(Long userId);
}
