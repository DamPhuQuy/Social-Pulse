package com.socialpulse.app.post.application.service;

import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.PostCode;
import com.socialpulse.app.post.application.usecase.DeletePostUseCase;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.security.user.CustomUserDetails;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeletePostService implements DeletePostUseCase {

    private final PostRepository postRepository;

    public DeletePostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Override
    @Transactional
    public void deletePost(Long postId, CustomUserDetails currentUser) {
        log.info("User {} is attempting to delete post {}", currentUser.getId(), postId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(PostCode.POST_NOT_FOUND));

        boolean isAuthor = post.getUserId().equals(currentUser.getId());
        boolean hasManagePermission = currentUser.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("post:manage"));

        if (!isAuthor && !hasManagePermission) {
            log.warn("User {} is not authorized to delete post {}", currentUser.getId(), postId);
            throw new AppException(PostCode.POST_NOT_ACCESSIBLE);
        }

        postRepository.deleteById(postId);
        log.info("Post {} deleted successfully by user {}", postId, currentUser.getId());
    }
}
