package com.socialpulse.app.user.infrastructure.persistence.mapper;

import com.socialpulse.app.user.domain.model.Permission;
import com.socialpulse.app.user.infrastructure.persistence.entity.PermissionEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PermissionPersistenceMapper {

    Permission toDomain(PermissionEntity entity);

    PermissionEntity toEntity(Permission permission);
}
