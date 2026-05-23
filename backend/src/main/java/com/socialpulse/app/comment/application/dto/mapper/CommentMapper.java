package com.socialpulse.app.comment.application.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


import com.socialpulse.app.comment.application.dto.request.CommentCreationRequest;
import com.socialpulse.app.comment.application.dto.response.CommentReactionResponse;
import com.socialpulse.app.comment.application.dto.response.CommentCreationResponse;
import com.socialpulse.app.comment.domain.model.Comment;
import com.socialpulse.app.comment.domain.model.CommentReaction;
import com.socialpulse.app.common.utils.ReactionType;
import com.socialpulse.app.user.application.dto.response.UserSummary;
import com.socialpulse.app.user.domain.model.User;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "postId", source = "postId")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "parentCommentId", source = "parentCommentId")
    @Mapping(target = "content", source = "request.content")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "upvoteCount", constant = "0L")
    @Mapping(target = "downvoteCount", constant = "0L")
    @Mapping(target = "deleted", constant = "false")
    @Mapping(target = "edited", constant = "false")
    Comment toComment(Long postId, CommentCreationRequest request, Long userId, Long parentCommentId);

    @Mapping(target = "id", source = "comment.id")
    @Mapping(target = "postId", source = "comment.postId")
    @Mapping(target = "parentCommentId", source = "comment.parentCommentId")
    @Mapping(target = "content", source = "comment.content")
    @Mapping(target = "createdAt", source = "comment.createdAt")
    @Mapping(target = "edited", source = "comment.edited")
    @Mapping(target = "user", source = "user")
    @Mapping(target = "replyCount", source = "replyCount")
    CommentCreationResponse toCommentCreationResponse(Comment comment, User user, Integer replyCount);

    @Mapping(target = "id", source = "comment.id")
    @Mapping(target = "content", source = "comment.content")
    @Mapping(target = "createdAt", source = "comment.createdAt")
    @Mapping(target = "edited", source = "comment.edited")
    @Mapping(target = "user", source = "user")
    @Mapping(target = "replyCount", source = "replyCount")
    @Mapping(target = "upvoteCount", source = "comment.upvoteCount")
    @Mapping(target = "downvoteCount", source = "comment.downvoteCount")
    @Mapping(target = "myReaction", source = "myReaction")
    com.socialpulse.app.comment.application.dto.response.CommentResponse toCommentResponse(
            Comment comment,
            User user,
            Integer replyCount,
            ReactionType myReaction);

    @Mapping(target = "avatarUrl", source = "profile.avatarUrl")
    UserSummary toUserSummary(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "commentId", source = "commentId")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "reactionType", source = "reactionType")
    CommentReaction toCommentReaction(Long userId, Long commentId, ReactionType reactionType);

    @Mapping(target = "commentId", source = "commentId")
    @Mapping(target = "reactionType", source = "reactionType")
    CommentReactionResponse toCommentReactionResponse(CommentReaction reaction);
}
