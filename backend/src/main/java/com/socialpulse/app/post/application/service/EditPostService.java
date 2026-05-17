package com.socialpulse.app.post.application.service;

import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.PostCode;
import com.socialpulse.app.post.application.dto.mapper.PostMapper;
import com.socialpulse.app.post.application.dto.request.PostUpdateRequest;
import com.socialpulse.app.post.application.dto.response.PostUpdateResponse;
import com.socialpulse.app.post.application.usecase.EditPostUseCase;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.security.user.CustomUserDetails;

public class EditPostService implements EditPostUseCase {

    private final PostRepository postRepository;
    private final PostMapper postMapper;

    public EditPostService(PostRepository postRepository, PostMapper postMapper) {
        this.postRepository = postRepository;
        this.postMapper = postMapper;
    }

    @Override
    @Transactional
    public PostUpdateResponse editPost(Long postId, PostUpdateRequest request, CustomUserDetails currentUser) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(PostCode.POST_NOT_FOUND));

        boolean isAuthor = post.getUserId().equals(currentUser.getId());
        boolean hasManagePermission = currentUser.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("post:manage"));

        if (!isAuthor && !hasManagePermission) {
            throw new AppException(PostCode.POST_NOT_ACCESSIBLE);
        }

        post.update(request.getContent(), request.getImageUrl(), request.getImagePublicId(), request.getPrivacy(), request.getTopicId());

        Post updatedPost = postRepository.save(post);

        return postMapper.toPostUpdateResponse(updatedPost);
    }
}
