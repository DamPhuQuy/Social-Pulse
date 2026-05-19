package com.socialpulse.app.user.adapter.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.repository.UserRepository;
import com.socialpulse.app.user.infrastructure.persistence.mapper.UserPersistenceMapper;
import com.socialpulse.app.user.infrastructure.persistence.repository.JpaUserRepository;

public class UserRepositoryAdapter implements UserRepository {
    private final JpaUserRepository jpaUserRepository;
    private final UserPersistenceMapper userPersistenceMapper;

    public UserRepositoryAdapter(
        JpaUserRepository jpaUserRepository,
        UserPersistenceMapper userPersistenceMapper) {
        this.jpaUserRepository = jpaUserRepository;
        this.userPersistenceMapper = userPersistenceMapper;
    }

    @Override
    public User save(User user) {
        var userEntity = userPersistenceMapper.toEntity(user);

        return userPersistenceMapper.toDomain(jpaUserRepository.save(userEntity));
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpaUserRepository.findById(id).map(userPersistenceMapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaUserRepository.findByUsername(username).map(userPersistenceMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaUserRepository.findByEmail(email).map(userPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaUserRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaUserRepository.existsByEmail(email);
    }

    @Override
    public java.util.List<User> findAllById(java.util.List<Long> ids) {
        return jpaUserRepository.findAllById(ids)
                .stream()
                .map(userPersistenceMapper::toDomain)
                .toList();
    }
    public List<User> findByIds(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return jpaUserRepository.findAllByIdIn(ids).stream()
                .map(userPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Page<User> searchByQuery(String query, Pageable pageable) {
        return jpaUserRepository.searchByQuery(query, pageable)
                .map(userPersistenceMapper::toDomain);
    }

    @Override
    public Page<User> findAll(Pageable pageable) {
        return jpaUserRepository.findAll(pageable)
                .map(userPersistenceMapper::toDomain);
    }

    @Override
    public long countAll() {
        return jpaUserRepository.count();
    }

    @Override
    public long countByCreatedAtAfter(LocalDateTime since) {
        return jpaUserRepository.countByCreatedAtAfter(since);
    }

    @Override
    public Map<String, Long> countByStatus() {
        return jpaUserRepository.countGroupByStatus().stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> row[0].toString(),
                        row -> (Long) row[1]));
    }
}


