package com.socialpulse.app.post.application.service;

import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.auth.security.user.CustomUserDetails;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.PostCode;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.common.utils.ReactionType;
import com.socialpulse.app.post.application.dto.mapper.PostReactionMapper;
import com.socialpulse.app.post.application.dto.request.PostReactionRequest;
import com.socialpulse.app.post.application.dto.response.PostReactionResponse;
import com.socialpulse.app.post.application.port.in.ReactPostUseCase;
import com.socialpulse.app.post.application.port.out.PostReactionsRepositoryPort;
import com.socialpulse.app.post.application.port.out.PostRepositoryPort;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.model.PostReactions;
import com.socialpulse.app.user.application.port.out.UserRepositoryPort;

public class ReactPostService implements ReactPostUseCase {

    private final PostRepositoryPort postRepositoryPort;
    private final PostReactionsRepositoryPort postReactionsRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final PostReactionMapper postReactionMapper;

    public ReactPostService(PostRepositoryPort postRepositoryPort,
                            PostReactionsRepositoryPort postReactionsRepositoryPort,
                            UserRepositoryPort userRepositoryPort,
                            PostReactionMapper postReactionMapper) {
        this.postRepositoryPort = postRepositoryPort;
        this.postReactionsRepositoryPort = postReactionsRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.postReactionMapper = postReactionMapper;
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
            PostReactions newReaction = postReactionMapper.toPostReaction(currentUser.getId(), post.getId(), targetReaction);

            PostReactions savedReaction = postReactionsRepositoryPort.save(newReaction);
            incrementReactionCount(post, targetReaction);
            postRepositoryPort.save(post);

            return postReactionMapper.toPostReactionResponse(savedReaction);
        }

        if (currentReaction.getReactionType() == targetReaction) {
            postReactionsRepositoryPort.delete(currentReaction);
            decrementReactionCount(post, targetReaction);
            postRepositoryPort.save(post);
            return postReactionMapper.toPostReactionResponse(currentReaction);
        }

        decrementReactionCount(post, currentReaction.getReactionType());
        incrementReactionCount(post, targetReaction);
        currentReaction.changeReactionType(targetReaction);

        PostReactions updatedReaction = postReactionsRepositoryPort.save(currentReaction);
        postRepositoryPort.save(post);

        return postReactionMapper.toPostReactionResponse(updatedReaction);
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
