package com.socialpulse.app.follow.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.socialpulse.app.follow.domain.model.Follow;
import com.socialpulse.app.follow.infrastructure.persistence.entity.FollowEntity;
import com.socialpulse.app.user.infrastructure.persistence.entity.UserEntity;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FollowPersistenceMapper {
    @Mapping(target = "follower", ignore = true)
    @Mapping(target = "following", ignore = true)
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
