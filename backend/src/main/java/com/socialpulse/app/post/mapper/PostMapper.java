package com.socialpulse.app.post.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.socialpulse.app.post.dto.request.PostCreationRequest;
import com.socialpulse.app.post.dto.response.PostCreationResponse;
import com.socialpulse.app.post.entity.Post;
import com.socialpulse.app.user.entity.User;

@Mapper(componentModel = "spring")
public interface PostMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", source = "user")
    @Mapping(target = "upvoteCount", ignore = true)
    @Mapping(target = "downvoteCount", ignore = true)
    @Mapping(target = "cmtCount", ignore = true)
    @Mapping(target = "viewCount", ignore = true)
    @Mapping(target = "shareCount", ignore = true)
    @Mapping(target = "hotScore", ignore = true)
    @Mapping(target = "toxic", ignore = true)
    @Mapping(target = "toxicScore", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Post toPost(PostCreationRequest request, User user);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "userId", source = "user.id")
    PostCreationResponse toPostCreationResponse(Post post);
}
