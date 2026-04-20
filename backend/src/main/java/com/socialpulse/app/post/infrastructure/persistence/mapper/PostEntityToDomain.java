package com.socialpulse.app.post.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.model.PostReactions;
import com.socialpulse.app.post.infrastructure.persistence.entity.PostEntity;
import com.socialpulse.app.post.infrastructure.persistence.entity.PostReactionsEntity;

@Mapper(componentModel = "spring")
public interface PostEntityToDomain {

    @Mapping(target = "userId", source = "user.id")
    Post toDomain(PostEntity entity);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "postId", source = "post.id")
    PostReactions toDomain(PostReactionsEntity entity);
}
