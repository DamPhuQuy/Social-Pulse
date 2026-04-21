package com.socialpulse.app.user.domain.repository;

import java.util.Optional;

import com.socialpulse.app.user.domain.model.UserProfile;

public interface UserProfileRepository {
    Optional<UserProfile> findByUserId(Long userId);

    Optional<UserProfile> findByUsername(String username);
}

