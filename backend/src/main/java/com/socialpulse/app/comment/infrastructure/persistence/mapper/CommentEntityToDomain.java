package com.socialpulse.app.comment.infrastructure.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.socialpulse.app.comment.domain.model.Comment;
import com.socialpulse.app.comment.domain.model.CommentReaction;
import com.socialpulse.app.comment.infrastructure.persistence.entity.CommentEntity;
import com.socialpulse.app.comment.infrastructure.persistence.entity.CommentReactionEntity;

@Mapper(componentModel = "spring")
public interface CommentEntityToDomain {

    @Mapping(target = "postId", source = "post.id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "parentCommentId", source = "parentComment.id")
    @Mapping(target = "downvoteCount", source = "downVoteCount")
    Comment toDomain(CommentEntity entity);

    @Mapping(target = "commentId", source = "comment.id")
    @Mapping(target = "userId", source = "user.id")
    CommentReaction toDomain(CommentReactionEntity entity);
}
