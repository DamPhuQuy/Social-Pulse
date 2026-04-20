package com.socialpulse.app.auth.adapter.out;

import java.util.List;
import java.util.Optional;

import com.socialpulse.app.auth.application.port.out.RefreshTokenRepositoryPort;
import com.socialpulse.app.auth.domain.model.RefreshToken;
import com.socialpulse.app.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import com.socialpulse.app.auth.infrastructure.persistence.mapper.RefreshTokenMapper;
import com.socialpulse.app.auth.infrastructure.persistence.repository.JpaRefreshTokenRepository;

public class RefreshTokenRepositoryAdapter implements RefreshTokenRepositoryPort {

    private final JpaRefreshTokenRepository jpaRefreshTokenRepository;
    private final RefreshTokenMapper refreshTokenMapper;

    public RefreshTokenRepositoryAdapter(JpaRefreshTokenRepository jpaRefreshTokenRepository,
                                         RefreshTokenMapper refreshTokenMapper) {
        this.jpaRefreshTokenRepository = jpaRefreshTokenRepository;
        this.refreshTokenMapper = refreshTokenMapper;
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpaRefreshTokenRepository.findByTokenHash(tokenHash)
                .map(refreshTokenMapper::toDomain);
    }

    @Override
    public List<RefreshToken> findAllByUserIdAndRevokedAtIsNull(Long userId) {
        return jpaRefreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(userId)
                .stream()
                .map(refreshTokenMapper::toDomain)
                .toList();
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        RefreshTokenEntity saved = jpaRefreshTokenRepository.save(refreshTokenMapper.toEntity(token));
        return refreshTokenMapper.toDomain(saved);
    }

    @Override
    public List<RefreshToken> saveAll(List<RefreshToken> tokens) {
        List<RefreshTokenEntity> entities = tokens.stream()
                .map(refreshTokenMapper::toEntity)
                .toList();

        return jpaRefreshTokenRepository.saveAll(entities)
                .stream()
                .map(refreshTokenMapper::toDomain)
                .toList();
    }
}
