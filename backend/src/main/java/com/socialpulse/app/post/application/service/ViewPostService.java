package com.socialpulse.app.post.application.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.PostCode;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.common.utils.ReactionType;
import com.socialpulse.app.post.application.dto.mapper.PostMapper;
import com.socialpulse.app.post.application.dto.response.ViewPostResponse;
import com.socialpulse.app.post.application.usecase.ViewPostUseCase;
import com.socialpulse.app.post.domain.model.PostReactions;
import com.socialpulse.app.post.domain.repository.PostReactionsRepository;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.post.domain.enums.Privacy;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.security.user.CustomUserDetails;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ViewPostService implements ViewPostUseCase {

    public static final String VIEW_KEYS_SET = "views:delta:keys";
    public static final String VIEW_HLL_PREFIX = "post:%d:views:hll";
    public static final String VIEW_DELTA_PREFIX = "post:%d:views:delta";

    private final PostRepository postRepositoryPort;
    private final PostReactionsRepository postReactionsRepository;
    private final UserRepository userRepository;
    private final PostMapper postMapper;
    private final StringRedisTemplate redisTemplate;

    public ViewPostService(
            PostRepository postRepositoryPort,
            PostReactionsRepository postReactionsRepository,
            UserRepository userRepository,
            PostMapper postMapper,
            StringRedisTemplate redisTemplate) {
        this.postRepositoryPort = postRepositoryPort;
        this.postReactionsRepository = postReactionsRepository;
        this.userRepository = userRepository;
        this.postMapper = postMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public ViewPostResponse viewPost(Long postId, CustomUserDetails currentUser) {
        Post post = postRepositoryPort.findById(postId)
                .orElseThrow(() -> new AppException(PostCode.POST_NOT_FOUND));

        if (post.getDeletedAt() != null) {
            throw new AppException(PostCode.POST_NOT_FOUND);
        }
        Long userId = currentUser.getId();
        if (post.getPrivacy() != Privacy.PUBLIC && !post.getUserId().equals(userId)) {
            throw new AppException(PostCode.POST_NOT_ACCESSIBLE);
        }

        // Increment view count directly
        postRepositoryPort.incrementViewCount(postId);

        User author = userRepository.findById(post.getUserId())
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));
        Integer myVote = postReactionsRepository.findByPostIdAndUserId(postId, userId)
                .map(PostReactions::getReactionType)
                .map(this::toVote)
                .orElse(0);

        ViewPostResponse response = postMapper.toViewPostResponse(post, author, myVote);

        if (post.getType() == com.socialpulse.app.post.domain.enums.PostType.SHARE && post.getParentPostId() != null) {
            Post parent = postRepositoryPort.findById(post.getParentPostId()).orElse(null);
            if (parent != null && parent.getDeletedAt() == null) {
                User parentAuthor = userRepository.findById(parent.getUserId()).orElse(null);
                com.socialpulse.app.feed.application.dto.response.OriginalPostData originalPost = 
                    com.socialpulse.app.feed.application.dto.response.OriginalPostData.builder()
                        .postId(parent.getId())
                        .content(parent.getContent())
                        .imageUrl(parent.getImageUrl())
                        .topicSlugs(parent.getTopicSlugs())
                        .userId(parent.getUserId())
                        .username(parentAuthor != null ? parentAuthor.getUsername() : null)
                        .userAvatar(parentAuthor != null && parentAuthor.getProfile() != null ? parentAuthor.getProfile().getAvatarUrl() : null)
                        .createdAt(parent.getCreatedAt())
                        .build();
                response.setOriginalPost(originalPost);
            }
        }

        return response;
    }

    private void bufferUniqueView(Long postId, Long userId) {
        if (userId == null || redisTemplate == null) {
            return;
        }
        try {
            String hllKey = String.format(VIEW_HLL_PREFIX, postId);
            Long added = redisTemplate.opsForHyperLogLog().add(hllKey, String.valueOf(userId));
            if (added != null && added == 1L) {
                String deltaKey = String.format(VIEW_DELTA_PREFIX, postId);
                redisTemplate.opsForValue().increment(deltaKey, 1L);
                redisTemplate.opsForSet().add(VIEW_KEYS_SET, deltaKey);
            }
        } catch (Exception e) {
            log.warn("Failed to buffer unique view in Redis for post {}: {}", postId, e.getMessage());
        }
    }

    private int toVote(ReactionType reactionType) {
        if (reactionType == ReactionType.UPVOTE) {
            return 1;
        }
        if (reactionType == ReactionType.DOWNVOTE) {
            return -1;
        }
        return 0;
    }
}
