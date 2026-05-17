package com.socialpulse.app.bookmark.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.socialpulse.app.bookmark.domain.model.Bookmark;
import com.socialpulse.app.bookmark.infrastructure.persistence.entity.BookmarkEntity;
import com.socialpulse.app.post.infrastructure.persistence.entity.PostEntity;
import com.socialpulse.app.user.infrastructure.persistence.entity.UserEntity;

@Mapper(componentModel = "spring")
public interface BookmarkPersistenceMapper {
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "postId", source = "post.id")
    Bookmark toDomain(BookmarkEntity entity);

    @Mapping(target = "user", source = "userId", qualifiedByName = "userIdToUser")
    @Mapping(target = "post", source = "postId", qualifiedByName = "postIdToPost")
    BookmarkEntity toEntity(Bookmark domain);

    @Named("userIdToUser")
    default UserEntity userIdToUser(Long userId) {
        if (userId == null) {
            return null;
        }
        return UserEntity.builder().id(userId).build();
    }

    @Named("postIdToPost")
    default PostEntity postIdToPost(Long postId) {
        if (postId == null) {
            return null;
        }
        return PostEntity.builder().id(postId).build();
    }
}
