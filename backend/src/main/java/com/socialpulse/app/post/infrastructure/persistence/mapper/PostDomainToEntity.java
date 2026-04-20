package com.socialpulse.app.post.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.model.PostReactions;
import com.socialpulse.app.post.infrastructure.persistence.entity.PostEntity;
import com.socialpulse.app.post.infrastructure.persistence.entity.PostReactionsEntity;
import com.socialpulse.app.user.infrastructure.persistence.entity.UserEntity;

@Mapper(componentModel = "spring")
public interface PostDomainToEntity {

    @Mapping(target = "user", source = "userId", qualifiedByName = "userIdToUserEntity")
    PostEntity toEntity(Post domain);

    @Mapping(target = "user", source = "userId", qualifiedByName = "userIdToUserEntity")
    @Mapping(target = "post", source = "postId", qualifiedByName = "postIdToPostEntity")
    PostReactionsEntity toEntity(PostReactions domain);

    @Named("userIdToUserEntity")
    default UserEntity userIdToUserEntity(Long userId) {
        if (userId == null) {
            return null;
        }

        return UserEntity.builder().id(userId).build();
    }

    @Named("postIdToPostEntity")
    default PostEntity postIdToPostEntity(Long postId) {
        if (postId == null) {
            return null;
        }

        return PostEntity.builder().id(postId).build();
    }
}
