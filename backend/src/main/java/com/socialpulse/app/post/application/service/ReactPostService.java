package com.socialpulse.app.post.application.service;

import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.PostCode;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.common.utils.ReactionType;
import com.socialpulse.app.post.application.dto.mapper.PostMapper;
import com.socialpulse.app.post.application.dto.request.PostReactionRequest;
import com.socialpulse.app.post.application.dto.response.PostReactionResponse;
import com.socialpulse.app.post.application.usecase.ReactPostUseCase;
import com.socialpulse.app.post.domain.repository.PostReactionsRepository;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.model.PostReactions;
import com.socialpulse.app.security.user.CustomUserDetails;
import com.socialpulse.app.user.domain.repository.UserRepository;

public class ReactPostService implements ReactPostUseCase {

    private final PostRepository postRepositoryPort;
    private final PostReactionsRepository postReactionsRepositoryPort;
    private final UserRepository userRepositoryPort;
    private final PostMapper postMapper;

    public ReactPostService(PostRepository postRepositoryPort,
                            PostReactionsRepository postReactionsRepositoryPort,
                            UserRepository userRepositoryPort,
                            PostMapper postMapper) {
        this.postRepositoryPort = postRepositoryPort;
        this.postReactionsRepositoryPort = postReactionsRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.postMapper = postMapper;
    }

    @Override
    @Transactional
    public PostReactionResponse react(PostReactionRequest request, CustomUserDetails currentUser) {
        Post post = postRepositoryPort.findById(request.getPostId())
                .orElseThrow(() -> new AppException(PostCode.POST_NOT_FOUND));

        userRepositoryPort.findById(currentUser.getId())
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        PostReactions currentReaction = postReactionsRepositoryPort
                .findByPostIdAndUserId(request.getPostId(), currentUser.getId())
                .orElse(null);

        ReactionType targetReaction = request.getReactionType();

        if (currentReaction == null) {
            PostReactions newReaction = postMapper.toPostReaction(currentUser.getId(), post.getId(), targetReaction);

            PostReactions savedReaction = postReactionsRepositoryPort.save(newReaction);
            incrementReactionCount(post, targetReaction);
            postRepositoryPort.save(post);

            return postMapper.toPostReactionResponse(savedReaction);
        }

        if (currentReaction.getReactionType() == targetReaction) {
            postReactionsRepositoryPort.delete(currentReaction);
            decrementReactionCount(post, targetReaction);
            postRepositoryPort.save(post);
            return postMapper.toPostReactionResponse(currentReaction);
        }

        decrementReactionCount(post, currentReaction.getReactionType());
        incrementReactionCount(post, targetReaction);
        currentReaction.changeReactionType(targetReaction);

        PostReactions updatedReaction = postReactionsRepositoryPort.save(currentReaction);
        postRepositoryPort.save(post);

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


