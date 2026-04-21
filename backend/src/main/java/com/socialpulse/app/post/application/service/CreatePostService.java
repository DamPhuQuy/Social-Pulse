package com.socialpulse.app.post.application.service;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.post.application.dto.mapper.PostMapper;
import com.socialpulse.app.post.application.dto.request.PostCreationRequest;
import com.socialpulse.app.post.application.dto.response.PostCreationResponse;
import com.socialpulse.app.post.application.usecase.CreatePostUseCase;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.security.user.CustomUserDetails;
import com.socialpulse.app.user.domain.repository.UserRepository;

public class CreatePostService implements CreatePostUseCase {

    private final PostRepository postRepositoryPort;
    private final UserRepository userRepositoryPort;
    private final PostMapper postMapper;

    public CreatePostService(PostRepository postRepositoryPort,
                             UserRepository userRepositoryPort,
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


