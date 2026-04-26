package com.socialpulse.app.user.infrastructure.persistence.mapper;

import com.socialpulse.app.user.domain.model.Role;
import com.socialpulse.app.user.infrastructure.persistence.entity.RoleEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = PermissionPersistenceMapper.class)
public interface RolePersistenceMapper {

    Role toDomain(RoleEntity entity);

    RoleEntity toEntity(Role role);
}
