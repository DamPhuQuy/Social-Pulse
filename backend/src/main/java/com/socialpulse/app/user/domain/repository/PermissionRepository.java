package com.socialpulse.app.user.domain.repository;

import java.util.Optional;

import com.socialpulse.app.user.domain.model.Permission;

public interface PermissionRepository {
    Optional<Permission> findByName(String name);
    Permission save(Permission permission);
}
