package com.socialpulse.app.follow.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;

import com.socialpulse.app.follow.domain.model.Follow;
import com.socialpulse.app.follow.infrastructure.persistence.entity.FollowEntity;
import com.socialpulse.app.user.infrastructure.persistence.entity.UserEntity;

@Mapper(componentModel = "spring")
public interface FollowPersistenceMapper {
    FollowEntity toEntity(Follow follow);

    Follow toDomain(FollowEntity followEntity);

    default FollowEntity toEntity(Follow follow, UserEntity follower, UserEntity following) {
        if (follow == null) {
            return null;
        }
        FollowEntity entity = new FollowEntity();
        entity.setId(follow.getId());
        entity.setFollower(follower);
        entity.setFollowing(following);
        return entity;
    }
}