package com.socialpulse.app.share.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.socialpulse.app.share.domain.model.Share;
import com.socialpulse.app.share.infrastructure.persistence.entity.ShareEntity;

@Mapper(componentModel = "spring")
public interface ShareMapper {
    @Mapping(target = "postId", source = "post.id")
    @Mapping(target = "userId", source = "user.id")
    Share toDomain(ShareEntity shareEntity);

    @Mapping(target = "post.id", source = "postId")
    @Mapping(target = "user.id", source = "userId")
    ShareEntity toEntity(Share share);
}
