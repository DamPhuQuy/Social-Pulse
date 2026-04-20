package com.socialpulse.app.post.application.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.socialpulse.app.common.utils.ReactionType;
import com.socialpulse.app.post.application.dto.response.PostReactionResponse;
import com.socialpulse.app.post.domain.model.PostReactions;

@Mapper(componentModel = "spring")
public interface PostReactionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "postId", source = "postId")
    @Mapping(target = "reactionType", source = "reactionType")
    PostReactions toPostReaction(Long userId, Long postId, ReactionType reactionType);

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "postId", source = "postId")
    @Mapping(target = "reactionType", expression = "java(reaction.getReactionType() != null ? reaction.getReactionType().name() : null)")
    PostReactionResponse toPostReactionResponse(PostReactions reaction);
}
