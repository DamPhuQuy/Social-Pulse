package com.socialpulse.app.post.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.socialpulse.app.common.utils.ReactionType;
import com.socialpulse.app.post.dto.response.PostReactionResponse;
import com.socialpulse.app.post.entity.Post;
import com.socialpulse.app.post.entity.PostReactions;
import com.socialpulse.app.user.entity.User;

@Mapper(componentModel = "spring")
public interface PostReactionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "user", source = "user")
    @Mapping(target = "post", source = "post")
    @Mapping(target = "reactionType", source = "reactionType")
    PostReactions toPostReaction(User user, Post post, ReactionType reactionType);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "postId", source = "post.id")
    @Mapping(target = "reactionType", expression = "java(reaction.getReactionType() != null ? reaction.getReactionType().name() : null)")
    PostReactionResponse toPostReactionResponse(PostReactions reaction);
}
