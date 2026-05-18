package com.socialpulse.app.user.infrastructure.persistence.mapper;

import com.socialpulse.app.user.infrastructure.persistence.mapper.RolePersistenceMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.model.UserProfile;
import com.socialpulse.app.user.infrastructure.persistence.entity.UserEntity;
import com.socialpulse.app.user.infrastructure.persistence.entity.UserProfileEntity;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", uses = RolePersistenceMapper.class)
public interface UserPersistenceMapper {

    @Mapping(target = "isLocked", expression = "java(entity.isLocked())")
    User toDomain(UserEntity entity);

    @Mapping(target = "displayName", expression = "java(resolveDisplayName(entity))")
    UserProfile toDomain(UserProfileEntity entity);

    @Mapping(target = "isLocked", expression = "java(user.isLocked())")
    @Mapping(target = "profile", ignore = true)
    UserEntity toEntity(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", source = "id", qualifiedByName = "userIdToUserEntity")
    UserProfileEntity toEntity(UserProfile userProfile);

    default String resolveDisplayName(UserProfileEntity entity) {
        if (entity == null) {
            return null;
        }

        if (entity.getDisplayName() != null && !entity.getDisplayName().isBlank()) {
            return entity.getDisplayName();
        }

        if (entity.getUser() != null) {
            return entity.getUser().getUsername();
        }

        return null;
    }

    @Named("userIdToUserEntity")
    default UserEntity userIdToUserEntity(Long userId) {
        if (userId == null) {
            return null;
        }

        return UserEntity.builder().id(userId).build();
    }
}
