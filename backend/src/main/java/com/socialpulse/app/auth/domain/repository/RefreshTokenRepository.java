package com.socialpulse.app.auth.domain.repository;

import java.util.List;
import java.util.Optional;

import com.socialpulse.app.auth.domain.model.RefreshToken;

public interface RefreshTokenRepository {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findAllByUserIdAndRevokedAtIsNull(Long userId);

    RefreshToken save(RefreshToken token);

    List<RefreshToken> saveAll(List<RefreshToken> tokens);
}

