package com.socialpulse.app.post.application.service;

import com.socialpulse.app.realtime.application.service.SseEmitterRegistry;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.PostCode;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.post.application.dto.mapper.PostMapper;
import com.socialpulse.app.post.application.dto.request.PostCreationRequest;
import com.socialpulse.app.post.application.dto.response.PostCreationResponse;
import com.socialpulse.app.post.application.usecase.CreatePostUseCase;
import com.socialpulse.app.post.domain.enums.PostType;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.security.user.CustomUserDetails;
import com.socialpulse.app.user.domain.repository.UserRepository;

import java.util.Map;

public class CreatePostService implements CreatePostUseCase {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostMapper postMapper;
    private final StringRedisTemplate redisTemplate;
    private final SseEmitterRegistry sseEmitterRegistry;

    public CreatePostService(PostRepository postRepository,
                             UserRepository userRepository,
                             PostMapper postMapper,
                             StringRedisTemplate redisTemplate,
                             SseEmitterRegistry sseEmitterRegistry) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.postMapper = postMapper;
        this.redisTemplate = redisTemplate;
        this.sseEmitterRegistry = sseEmitterRegistry;
    }

    @Override
    @Transactional
    public PostCreationResponse createPost(PostCreationRequest request, CustomUserDetails currentUser) {
        userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        Post parentPost = null;
        Long parentPostId = null;

        // if it is shared, can not share a shared post
        if (request.getParentPostId() != null) {
            parentPost = postRepository.findById(request.getParentPostId())
                    .orElseThrow(() -> new AppException(PostCode.POST_NOT_FOUND));

            if (parentPost.isSharedPost()) {
                throw new AppException(PostCode.POST_ALREADY_SHARED);
            }

            if (parentPost.isPrivate() && !parentPost.getUserId().equals(currentUser.getId())) {
                throw new AppException(PostCode.POST_NOT_ACCESSIBLE);
            }

            parentPostId = request.getParentPostId();

            // check if the post is already shared by current user
            if (postRepository.existsByUserIdAndParentPostIdAndType(currentUser.getId(), parentPostId, PostType.SHARE)) {
                throw new AppException(PostCode.POST_ALREADY_SHARED);
            }

            String key = "post:" + parentPostId + ":shareCount:delta";
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        redisTemplate.opsForValue().increment(key);
                        redisTemplate.opsForSet().add("share:delta:keys", key);
                    }
                }
            );
        }

        Post post = Post.builder()
                .content(request.getContent())
                .imageUrl(request.getImageUrl())
                .imagePublicId(request.getImagePublicId())
                .topicSlugs(PostTopicCatalog.normalizeAndValidate(request.getTopicSlugs()))
                .userId(currentUser.getId())
                .privacy(request.getPrivacy())
                .parentPostId(parentPostId)
                .type(request.getParentPostId() == null ? PostType.ORIGINAL : PostType.SHARE)
                .topicId(request.getTopicId())
                .build();

        Post savedPost = postRepository.save(post);

        // CRITICAL FIX: Invalidate the user's feed cache so their newly created post
        // will be fetched immediately on the next feed request!
        redisTemplate.delete("user:feed:" + currentUser.getId());

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sseEmitterRegistry.broadcast("feed_refresh", Map.of(
                            "postId", savedPost.getId(),
                            "authorId", savedPost.getUserId(),
                            "reason", "POST_CREATED"
                    ));
                }
            });
        }

        return postMapper.toPostCreationResponse(savedPost);
    }
}


