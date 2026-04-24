package com.socialpulse.app.post.application.service;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.PostCode;
import com.socialpulse.app.post.application.dto.mapper.PostMapper;
import com.socialpulse.app.post.application.dto.response.ViewPostResponse;
import com.socialpulse.app.post.application.usecase.ViewPostUseCase;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.post.domain.enums.Privacy;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.security.user.CustomUserDetails;

public class ViewPostService implements ViewPostUseCase {

    private final PostRepository postRepositoryPort;
    private final PostMapper postMapper;

    public ViewPostService(PostRepository postRepositoryPort, PostMapper postMapper) {
        this.postRepositoryPort = postRepositoryPort;
        this.postMapper = postMapper;
    }

    @Override
    public ViewPostResponse viewPost(Long postId, CustomUserDetails currentUser) {
        Post post = postRepositoryPort.findById(postId)
                .orElseThrow(() -> new AppException(PostCode.POST_NOT_FOUND));

        if(post.getDeletedAt() != null){
            throw new AppException(PostCode.POST_NOT_FOUND);
        }
        Long userId = currentUser.getId();
        if (post.getPrivacy() != Privacy.PUBLIC && !post.getUserId().equals(userId)) {
            throw new AppException(PostCode.POST_NOT_ACCESSIBLE);
        }

        return postMapper.toViewPostResponse(post);
    }
}


