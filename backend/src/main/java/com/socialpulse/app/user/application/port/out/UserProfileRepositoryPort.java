package com.socialpulse.app.user.application.port.out;

import java.util.Optional;

import com.socialpulse.app.user.domain.model.UserProfile;

public interface UserProfileRepositoryPort {
    Optional<UserProfile> findByUserId(Long userId);

    Optional<UserProfile> findByUsername(String username);
}
