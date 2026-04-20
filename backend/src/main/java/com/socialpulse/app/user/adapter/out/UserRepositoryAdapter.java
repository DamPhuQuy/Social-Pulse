package com.socialpulse.app.user.adapter.out;

import java.util.Optional;

import com.socialpulse.app.user.application.port.out.UserRepositoryPort;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.infrastructure.persistence.mapper.UserEntityToDomainMapper;
import com.socialpulse.app.user.infrastructure.persistence.repository.JpaUserRepository;

public class UserRepositoryAdapter implements UserRepositoryPort {
    private final JpaUserRepository jpaUserRepository;
    private final UserEntityToDomainMapper userEntityToDomainMapper;

    public UserRepositoryAdapter(JpaUserRepository jpaUserRepository, UserEntityToDomainMapper userEntityToDomainMapper) {
        this.jpaUserRepository = jpaUserRepository;
        this.userEntityToDomainMapper = userEntityToDomainMapper;
    }

    @Override
    public User save(User user) {
        var userEntity = userEntityToDomainMapper.toEntity(user);

        return userEntityToDomainMapper.toDomain(jpaUserRepository.save(userEntity));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaUserRepository.findByUsername(username).map(userEntityToDomainMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaUserRepository.findByEmail(email).map(userEntityToDomainMapper::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaUserRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaUserRepository.existsByEmail(email);
    }
}
