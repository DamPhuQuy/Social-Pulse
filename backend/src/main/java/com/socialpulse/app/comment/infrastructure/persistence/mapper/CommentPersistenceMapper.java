package com.socialpulse.app.comment.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.socialpulse.app.comment.domain.model.Comment;
import com.socialpulse.app.comment.domain.model.CommentReaction;
import com.socialpulse.app.comment.infrastructure.persistence.entity.CommentEntity;
import com.socialpulse.app.comment.infrastructure.persistence.entity.CommentReactionEntity;
import com.socialpulse.app.post.infrastructure.persistence.entity.PostEntity;
import com.socialpulse.app.user.infrastructure.persistence.entity.UserEntity;

@Mapper(componentModel = "spring")
public interface CommentPersistenceMapper {

    @Mapping(target = "postId", source = "post.id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "parentCommentId", source = "parentComment.id")
    @Mapping(target = "downvoteCount", source = "downVoteCount")
    Comment toDomain(CommentEntity entity);

    @Mapping(target = "commentId", source = "comment.id")
    @Mapping(target = "userId", source = "user.id")
    CommentReaction toDomain(CommentReactionEntity entity);

    @Mapping(target = "post", source = "postId", qualifiedByName = "postIdToPost")
    @Mapping(target = "user", source = "userId", qualifiedByName = "userIdToUser")
    @Mapping(target = "parentComment", source = "parentCommentId", qualifiedByName = "commentIdToCommentEntity")
    @Mapping(target = "replies", ignore = true)
    @Mapping(target = "downVoteCount", source = "downvoteCount")
    CommentEntity toEntity(Comment domain);

    @Mapping(target = "comment", source = "commentId", qualifiedByName = "commentIdToCommentEntity")
    @Mapping(target = "user", source = "userId", qualifiedByName = "userIdToUser")
    CommentReactionEntity toEntity(CommentReaction domain);

    @Named("postIdToPost")
    default PostEntity postIdToPost(Long postId) {
        if (postId == null) {
            return null;
        }

        return PostEntity.builder().id(postId).build();
    }

    @Named("userIdToUser")
    default UserEntity userIdToUser(Long userId) {
        if (userId == null) {
            return null;
        }

        return UserEntity.builder().id(userId).build();
    }

    @Named("commentIdToCommentEntity")
    default CommentEntity commentIdToCommentEntity(Long commentId) {
        if (commentId == null) {
            return null;
        }

        return CommentEntity.builder().id(commentId).build();
    }
}
