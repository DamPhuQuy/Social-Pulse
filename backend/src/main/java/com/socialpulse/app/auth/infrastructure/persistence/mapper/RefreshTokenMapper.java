package com.socialpulse.app.auth.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.socialpulse.app.auth.domain.model.RefreshToken;
import com.socialpulse.app.auth.infrastructure.persistence.entity.RefreshTokenEntity;
import com.socialpulse.app.user.infrastructure.persistence.entity.UserEntity;

@Mapper(componentModel = "spring")
public interface RefreshTokenMapper {

    @Mapping(target = "userId", source = "user.id")
    RefreshToken toDomain(RefreshTokenEntity entity);

    @Mapping(target = "user", source = "userId", qualifiedByName = "userIdToUser")
    RefreshTokenEntity toEntity(RefreshToken domain);

    @Named("userIdToUser")
    default UserEntity userIdToUser(Long userId) {
        if (userId == null) {
            return null;
        }

        return UserEntity.builder().id(userId).build();
    }
}
