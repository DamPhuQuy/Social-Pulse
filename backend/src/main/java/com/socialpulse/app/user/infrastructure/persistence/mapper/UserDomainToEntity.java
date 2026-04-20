package com.socialpulse.app.user.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.model.UserProfile;
import com.socialpulse.app.user.infrastructure.persistence.entity.UserEntity;
import com.socialpulse.app.user.infrastructure.persistence.entity.UserProfileEntity;

@Mapper(componentModel = "spring")
public interface UserDomainToEntity {
    @Mapping(target = "isLocked", expression = "java(user.isLocked())")
    @Mapping(target = "profile", ignore = true)
    UserEntity toEntity(User user);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "displayName", ignore = true)
    UserProfileEntity toEntity(UserProfile userProfile);
}
