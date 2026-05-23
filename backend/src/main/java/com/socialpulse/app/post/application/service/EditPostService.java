package com.socialpulse.app.post.application.service;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.PostCode;
import com.socialpulse.app.post.application.dto.mapper.PostMapper;
import com.socialpulse.app.post.application.dto.request.PostUpdateRequest;
import com.socialpulse.app.post.application.dto.response.PostUpdateResponse;
import com.socialpulse.app.post.application.usecase.EditPostUseCase;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.realtime.application.service.SseEmitterRegistry;
import com.socialpulse.app.security.user.CustomUserDetails;

import java.util.Map;

public class EditPostService implements EditPostUseCase {

    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final StringRedisTemplate redisTemplate;
    private final SseEmitterRegistry sseEmitterRegistry;

    public EditPostService(PostRepository postRepository, PostMapper postMapper, StringRedisTemplate redisTemplate, SseEmitterRegistry sseEmitterRegistry) {
        this.postRepository = postRepository;
        this.postMapper = postMapper;
        this.redisTemplate = redisTemplate;
        this.sseEmitterRegistry = sseEmitterRegistry;
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

        post.update(
                request.getContent(),
                request.getImageUrl(),
                request.getImagePublicId(),
                request.getPrivacy(),
                PostTopicCatalog.normalizeAndValidate(request.getTopicSlugs()));

        Post updatedPost = postRepository.save(post);
        redisTemplate.delete("user:feed:" + currentUser.getId());

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sseEmitterRegistry.broadcast("feed_refresh", Map.of(
                            "postId", updatedPost.getId(),
                            "authorId", updatedPost.getUserId(),
                            "reason", "POST_UPDATED"
                    ));
                }
            });
        }

        return postMapper.toPostUpdateResponse(updatedPost);
    }
}
