package com.socialpulse.app.post.application.service;

import com.socialpulse.app.auth.security.user.CustomUserDetails;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.post.application.dto.mapper.PostMapper;
import com.socialpulse.app.post.application.dto.request.PostCreationRequest;
import com.socialpulse.app.post.application.dto.response.PostCreationResponse;
import com.socialpulse.app.post.application.port.in.CreatePostUseCase;
import com.socialpulse.app.post.application.port.out.PostRepositoryPort;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.user.application.port.out.UserRepositoryPort;

public class CreatePostService implements CreatePostUseCase {

    private final PostRepositoryPort postRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final PostMapper postMapper;

    public CreatePostService(PostRepositoryPort postRepositoryPort,
                             UserRepositoryPort userRepositoryPort,
                             PostMapper postMapper) {
        this.postRepositoryPort = postRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.postMapper = postMapper;
    }

    @Override
    public PostCreationResponse createPost(PostCreationRequest request, CustomUserDetails currentUser) {
        userRepositoryPort.findById(currentUser.getId())
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        Post post = postMapper.toPost(request, currentUser.getId());

        Post savedPost = postRepositoryPort.save(post);

        return postMapper.toPostCreationResponse(savedPost);
    }
}
