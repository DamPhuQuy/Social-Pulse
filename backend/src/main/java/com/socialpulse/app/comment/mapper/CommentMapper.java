package com.socialpulse.app.comment.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.socialpulse.app.comment.dto.request.CommentCreationRequest;
import com.socialpulse.app.comment.dto.response.CommentCreationResponse;
import com.socialpulse.app.comment.entity.Comment;
import com.socialpulse.app.post.entity.Post;
import com.socialpulse.app.user.dto.response.UserSummary;
import com.socialpulse.app.user.entity.User;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "post", source = "post")
    @Mapping(target = "user", source = "user")
    @Mapping(target = "parentComment", source = "parentComment")
    @Mapping(target = "content", source = "request.content")
    @Mapping(target = "replies", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "upvoteCount", ignore = true)
    @Mapping(target = "downVoteCount", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    Comment toComment(CommentCreationRequest request, Post post, User user, Comment parentComment);

    @Mapping(target = "postId", source = "post.id")
    @Mapping(target = "parentCommentId", source = "parentComment.id")
    @Mapping(target = "replyCount", constant = "0")
    CommentCreationResponse toCommentCreationResponse(Comment comment);

    @Mapping(target = "avatarUrl", ignore = true)
    UserSummary toUserSummary(User user);
}
