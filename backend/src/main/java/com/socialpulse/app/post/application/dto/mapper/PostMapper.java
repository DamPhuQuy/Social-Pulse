package com.socialpulse.app.post.application.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.socialpulse.app.post.application.dto.request.PostCreationRequest;
import com.socialpulse.app.post.application.dto.response.PostCreationResponse;
import com.socialpulse.app.post.application.dto.response.ViewPostResponse;
import com.socialpulse.app.post.domain.model.Post;

@Mapper(componentModel = "spring")
public interface PostMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", source = "userId")
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

    ViewPostResponse toViewPostResponse(Post post);
}
