package com.socialpulse.app.user.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.socialpulse.app.user.domain.model.Role;
import com.socialpulse.app.user.infrastructure.persistence.entity.RoleEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = PermissionPersistenceMapper.class)
public interface RolePersistenceMapper {

    Role toDomain(RoleEntity entity);

    @Mapping(target = "users", ignore = true)
    RoleEntity toEntity(Role role);
}
