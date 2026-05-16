package com.socialpulse.app.user.domain.repository;

import com.socialpulse.app.user.domain.model.Permission;

import java.util.Optional;

public interface PermissionRepository {
    Optional<Permission> findByName(String name);
}
