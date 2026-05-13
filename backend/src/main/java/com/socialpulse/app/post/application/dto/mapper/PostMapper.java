package com.socialpulse.app.post.application.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.socialpulse.app.common.utils.ReactionType;
import com.socialpulse.app.post.application.dto.request.PostCreationRequest;
import com.socialpulse.app.post.application.dto.response.PostCreationResponse;
import com.socialpulse.app.post.application.dto.response.PostReactionResponse;
import com.socialpulse.app.post.application.dto.response.PostUpdateResponse;
import com.socialpulse.app.post.application.dto.response.ViewPostResponse;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.model.PostReactions;

@Mapper(componentModel = "spring")
public interface PostMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "parentPostId", ignore = true)
    @Mapping(target = "type", constant = "ORIGINAL")
    @Mapping(target = "upvoteCount", constant = "0L")
    @Mapping(target = "downvoteCount", constant = "0L")
    @Mapping(target = "cmtCount", constant = "0L")
    @Mapping(target = "viewCount", constant = "0L")
    @Mapping(target = "shareCount", constant = "0L")
    @Mapping(target = "hotScore", constant = "0.0")
    @Mapping(target = "toxic", constant = "false")
    @Mapping(target = "toxicScore", constant = "0.0")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Post toPost(PostCreationRequest request, Long userId);

    PostCreationResponse toPostCreationResponse(Post post);

    @Mapping(target = "username", ignore = true)
    @Mapping(target = "userAvatar", ignore = true)
    @Mapping(target = "myVote", ignore = true)
    ViewPostResponse toViewPostResponse(Post post);

    PostUpdateResponse toPostUpdateResponse(Post post);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "postId", source = "postId")
    @Mapping(target = "reactionType", source = "reactionType")
    PostReactions toPostReaction(Long userId, Long postId, ReactionType reactionType);

    @Mapping(target = "postId", source = "postId")
    @Mapping(target = "reactionType", source = "reactionType")
    PostReactionResponse toPostReactionResponse(PostReactions reaction);
}
