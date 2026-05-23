package com.socialpulse.app.user.infrastructure.persistence.mapper;


import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.model.UserProfile;
import com.socialpulse.app.user.infrastructure.persistence.entity.UserEntity;
import com.socialpulse.app.user.infrastructure.persistence.entity.UserProfileEntity;
import org.mapstruct.Named;

import com.socialpulse.app.topic.infrastructure.persistence.mapper.TopicPersistenceMapper;

@Mapper(componentModel = "spring", uses = {RolePersistenceMapper.class, TopicPersistenceMapper.class})
public interface UserPersistenceMapper {

    @Mapping(target = "isLocked", expression = "java(entity.isLocked())")
    User toDomain(UserEntity entity);

    @Mapping(target = "displayName", expression = "java(resolveDisplayName(entity))")
    UserProfile toDomain(UserProfileEntity entity);

    @Mapping(target = "isLocked", expression = "java(user.isLocked())")
    @Mapping(target = "profile", source = "profile")
    UserEntity toEntity(User user);

    @Mapping(target = "id", source = "id")
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
    @AfterMapping
    default void linkProfile(@MappingTarget UserEntity entity) {
        if (entity.getProfile() != null) {
            entity.getProfile().setUser(entity);
        }
    }
}
