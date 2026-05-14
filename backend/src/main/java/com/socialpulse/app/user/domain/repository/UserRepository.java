package com.socialpulse.app.user.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.socialpulse.app.user.domain.model.User;

public interface UserRepository {
    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);
    
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    java.util.List<User> findAllById(java.util.List<Long> ids);
    List<User> findByIds(Set<Long> ids);
}


