package com.socialpulse.app.post.application.service;

import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.PostCode;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.common.utils.ReactionType;
import com.socialpulse.app.feed.domain.repository.UserInteractionRepository;
import com.socialpulse.app.notification.application.service.NotificationCommandService;
import com.socialpulse.app.post.application.dto.mapper.PostMapper;
import com.socialpulse.app.post.application.dto.request.PostReactionRequest;
import com.socialpulse.app.post.application.dto.response.PostReactionResponse;
import com.socialpulse.app.post.application.usecase.ReactPostUseCase;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.model.PostReactions;
import com.socialpulse.app.post.domain.repository.PostReactionsRepository;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.security.user.CustomUserDetails;
import com.socialpulse.app.user.domain.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReactPostService implements ReactPostUseCase {

    private final PostRepository postRepository;
    private final PostReactionsRepository postReactionsRepository;
    private final UserRepository userRepository;
    private final PostMapper postMapper;
    private final NotificationCommandService notificationCommandService;
    private final UserInteractionRepository userInteractionRepository;

    public ReactPostService(PostRepository postRepository,
                            PostReactionsRepository postReactionsRepository,
                            UserRepository userRepository,
                            PostMapper postMapper,
                            NotificationCommandService notificationCommandService,
                            UserInteractionRepository userInteractionRepository) {
        this.postRepository = postRepository;
        this.postReactionsRepository = postReactionsRepository;
        this.userRepository = userRepository;
        this.postMapper = postMapper;
        this.notificationCommandService = notificationCommandService;
        this.userInteractionRepository = userInteractionRepository;
    }

    @Override
    @Transactional
    public PostReactionResponse react(PostReactionRequest request, CustomUserDetails currentUser) {
        log.info("User {} is reacting to post {} with type {}", currentUser.getId(), request.getPostId(), request.getReactionType());
        Post post = postRepository.findById(request.getPostId())
                .orElseThrow(() -> new AppException(PostCode.POST_NOT_FOUND));

        userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        PostReactions currentReaction = postReactionsRepository
                .findByPostIdAndUserId(request.getPostId(), currentUser.getId())
                .orElse(null);

        ReactionType targetReaction = request.getReactionType();

        if (currentReaction == null) {
            PostReactions newReaction = postMapper.toPostReaction(currentUser.getId(), post.getId(), targetReaction);

            PostReactions savedReaction = postReactionsRepository.save(newReaction);
            incrementReactionCount(post, targetReaction);
            postRepository.save(post);
            notificationCommandService.notifyPostReaction(currentUser.getId(), post.getUserId(), post.getId(), targetReaction);

            // Record interaction for personalized feed ranking
            if (targetReaction == ReactionType.UPVOTE && !currentUser.getId().equals(post.getUserId())) {
                userInteractionRepository.save(currentUser.getId(), post.getUserId(), "UPVOTE");
            }

            log.debug("New reaction saved for user {} on post {}", currentUser.getId(), post.getId());
            return postMapper.toPostReactionResponse(savedReaction);
        }

        if (currentReaction.getReactionType() == targetReaction) {
            postReactionsRepository.delete(currentReaction);
            decrementReactionCount(post, targetReaction);
            postRepository.save(post);
            log.debug("Reaction removed for user {} on post {}", currentUser.getId(), post.getId());
            return postMapper.toPostReactionResponse(currentReaction);
        }

        decrementReactionCount(post, currentReaction.getReactionType());
        incrementReactionCount(post, targetReaction);
        currentReaction.changeReactionType(targetReaction);

        PostReactions updatedReaction = postReactionsRepository.save(currentReaction);
        postRepository.save(post);
        notificationCommandService.notifyPostReaction(currentUser.getId(), post.getUserId(), post.getId(), targetReaction);

        log.debug("Reaction updated for user {} on post {} to {}", currentUser.getId(), post.getId(), targetReaction);
        return postMapper.toPostReactionResponse(updatedReaction);
    }

    private void incrementReactionCount(Post post, ReactionType reactionType) {
        if (reactionType == ReactionType.UPVOTE) {
            post.incrementUpvoteCount();
            return;
        }

        if (reactionType == ReactionType.DOWNVOTE) {
            post.incrementDownvoteCount();
        }
    }

    private void decrementReactionCount(Post post, ReactionType reactionType) {
        if (reactionType == ReactionType.UPVOTE) {
            post.decrementUpvoteCount();
            return;
        }

        if (reactionType == ReactionType.DOWNVOTE) {
            post.decrementDownvoteCount();
        }
    }

}

