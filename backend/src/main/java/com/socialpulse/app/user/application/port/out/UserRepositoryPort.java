package com.socialpulse.app.user.application.port.out;

import java.util.Optional;

import com.socialpulse.app.user.domain.model.User;

public interface UserRepositoryPort {
    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
