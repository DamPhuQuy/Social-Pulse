package com.socialpulse.app.user.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.socialpulse.app.user.domain.model.Permission;
import com.socialpulse.app.user.infrastructure.persistence.entity.PermissionEntity;

@Mapper(componentModel = "spring")
public interface PermissionPersistenceMapper {

    Permission toDomain(PermissionEntity entity);

    @Mapping(target = "roles", ignore = true)
    PermissionEntity toEntity(Permission permission);
}