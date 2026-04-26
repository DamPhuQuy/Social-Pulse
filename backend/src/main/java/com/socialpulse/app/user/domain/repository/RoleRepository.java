package com.socialpulse.app.user.domain.repository;

import com.socialpulse.app.user.domain.model.Role;

import java.util.Optional;

public interface RoleRepository {
    Optional<Role> findByName(String name);
    Role save(Role role);
}
