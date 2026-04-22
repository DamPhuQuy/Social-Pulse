package com.socialpulse.app.share.application.service;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.PostCode;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.post.domain.enums.PostType;
import com.socialpulse.app.post.domain.enums.Privacy;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.security.user.CustomUserDetails;
import com.socialpulse.app.share.application.dto.request.ShareCreationRequest;
import com.socialpulse.app.share.application.dto.response.ShareCreationResponse;
import com.socialpulse.app.share.application.usecase.ShareUseCase;
import com.socialpulse.app.user.domain.repository.UserRepository;

import jakarta.transaction.Transactional;

public class SharePostService implements ShareUseCase {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public SharePostService(PostRepository postRepository, UserRepository userRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ShareCreationResponse createShare(ShareCreationRequest request, CustomUserDetails currentUser) {
        userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        Post parentPost = postRepository.findById(request.getPostId())
                .orElseThrow(() -> new AppException(PostCode.POST_NOT_FOUND));

        if (parentPost.getPrivacy() != Privacy.PUBLIC && !parentPost.getUserId().equals(currentUser.getId())) {
            throw new AppException(PostCode.POST_NOT_ACCESSIBLE);
        }

        Long rootPostId = resolveRootPostId(parentPost);
        boolean alreadyShared = postRepository.existsByUserIdAndParentPostIdAndType(
                currentUser.getId(),
                rootPostId,
                PostType.SHARE
        );

        if (alreadyShared) {
            throw new AppException(PostCode.POST_ALREADY_SHARED);
        }

        Post countingPost = parentPost;
        if (!parentPost.getId().equals(rootPostId)) {
            countingPost = postRepository.findById(rootPostId)
                    .orElseThrow(() -> new AppException(PostCode.POST_NOT_FOUND));
        }

        countingPost.incrementShareCount();
        postRepository.save(countingPost);

        Post sharePost = Post.builder()
                .userId(currentUser.getId())
                .content(normalizeContent(request.getContent()))
                .imageUrl(null)
                .imagePublicId(null)
                .parentPostId(rootPostId)
                .type(PostType.SHARE)
                .privacy(request.getPrivacy() == null ? Privacy.PUBLIC : request.getPrivacy())
                .upvoteCount(0L)
                .downvoteCount(0L)
                .cmtCount(0L)
                .viewCount(0L)
                .shareCount(0L)
                .hotScore(0.0D)
                .toxic(false)
                .toxicScore(0.0D)
                .build();

        Post savedShare = postRepository.save(sharePost);

        return ShareCreationResponse.builder()
                .id(savedShare.getId())
                .parentPostId(savedShare.getParentPostId())
                .userId(savedShare.getUserId())
                .content(savedShare.getContent())
                .privacy(savedShare.getPrivacy())
                .type(savedShare.getType())
                .createdAt(savedShare.getCreatedAt())
                .build();
    }

    private Long resolveRootPostId(Post post) {
        if (post.getType() == PostType.SHARE && post.getParentPostId() != null) {
            return post.getParentPostId();
        }
        return post.getId();
    }

    private String normalizeContent(String content) {
        if (content == null) {
            return null;
        }

        String trimmed = content.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
