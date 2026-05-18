package com.socialpulse.app.user.adapter.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.socialpulse.app.user.domain.model.Permission;
import com.socialpulse.app.user.domain.repository.PermissionRepository;
import com.socialpulse.app.user.infrastructure.persistence.mapper.PermissionPersistenceMapper;
import com.socialpulse.app.user.infrastructure.persistence.repository.JpaPermissionRepository;

@Component
public class PermissionRepositoryAdapter implements PermissionRepository {

    private final JpaPermissionRepository jpaPermissionRepository;
    private final PermissionPersistenceMapper permissionPersistenceMapper;

    public PermissionRepositoryAdapter(JpaPermissionRepository jpaPermissionRepository, PermissionPersistenceMapper permissionPersistenceMapper) {
        this.jpaPermissionRepository = jpaPermissionRepository;
        this.permissionPersistenceMapper = permissionPersistenceMapper;
    }

    @Override
    public Optional<Permission> findByName(String name) {
        return jpaPermissionRepository.findByName(name)
                .map(permissionPersistenceMapper::toDomain);
    }

    @Override
    public Permission save(Permission permission) {
        return permissionPersistenceMapper.toDomain(
                jpaPermissionRepository.save(permissionPersistenceMapper.toEntity(permission))
        );
    }
}
