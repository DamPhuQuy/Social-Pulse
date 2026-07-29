package com.socialpulse.app.comment.application.service;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.comment.application.dto.mapper.CommentMapper;
import com.socialpulse.app.comment.application.dto.request.CommentReactionRequest;
import com.socialpulse.app.comment.application.dto.response.CommentReactionResponse;
import com.socialpulse.app.comment.application.usecase.ReactCommentUseCase;
import com.socialpulse.app.comment.domain.model.Comment;
import com.socialpulse.app.comment.domain.model.CommentReaction;
import com.socialpulse.app.comment.domain.repository.CommentReactionRepository;
import com.socialpulse.app.comment.domain.repository.CommentRepository;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.CommentCode;
import com.socialpulse.app.common.exception.status.PostCode;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.common.utils.ReactionType;
import com.socialpulse.app.notification.application.service.NotificationCommandService;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.security.user.CustomUserDetails;
import com.socialpulse.app.user.domain.repository.UserRepository;

@Service
public class ReactCommentService implements ReactCommentUseCase {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final CommentReactionRepository commentReactionRepository;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;
    private final NotificationCommandService notificationCommandService;

    public ReactCommentService(
            CommentRepository commentRepository,
            PostRepository postRepository,
            CommentReactionRepository commentReactionRepository,
            UserRepository userRepository,
            CommentMapper commentMapper,
            NotificationCommandService notificationCommandService) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.commentReactionRepository = commentReactionRepository;
        this.userRepository = userRepository;
        this.commentMapper = commentMapper;
        this.notificationCommandService = notificationCommandService;
    }

    @Override
    @Transactional
    public CommentReactionResponse react(
            Long postId,
            Long commentId,
            CommentReactionRequest request,
            CustomUserDetails currentUser) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(PostCode.POST_NOT_FOUND));
        validatePostAccessible(post, currentUser);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(CommentCode.COMMENT_NOT_FOUND));

        if (!comment.getPostId().equals(postId)) {
            throw new AppException(CommentCode.COMMENT_NOT_BELONG_TO_POST);
        }

        if (comment.isDeleted()) {
            throw new AppException(CommentCode.COMMENT_ALREADY_DELETED);
        }

        userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        CommentReaction currentReaction = commentReactionRepository
                .findByCommentIdAndUserId(commentId, currentUser.getId())
                .orElse(null);

        ReactionType targetReaction = request.getReactionType();

        if (currentReaction == null) {
            CommentReaction newReaction = commentMapper.toCommentReaction(currentUser.getId(), commentId, targetReaction);
            CommentReaction savedReaction = commentReactionRepository.save(newReaction);
            incrementReactionCount(comment, targetReaction);
            commentRepository.save(comment);
            notificationCommandService.notifyCommentReaction(currentUser.getId(), comment.getUserId(), commentId, targetReaction);
            return commentMapper.toCommentReactionResponse(savedReaction);
        }

        if (currentReaction.getReactionType() == targetReaction) {
            commentReactionRepository.delete(currentReaction);
            decrementReactionCount(comment, targetReaction);
            commentRepository.save(comment);
            return commentMapper.toCommentReactionResponse(currentReaction);
        }

        decrementReactionCount(comment, currentReaction.getReactionType());
        incrementReactionCount(comment, targetReaction);
        currentReaction.changeReactionType(targetReaction);

        CommentReaction updatedReaction = commentReactionRepository.save(currentReaction);
        commentRepository.save(comment);
        notificationCommandService.notifyCommentReaction(currentUser.getId(), comment.getUserId(), commentId, targetReaction);
        return commentMapper.toCommentReactionResponse(updatedReaction);
    }

    private void incrementReactionCount(Comment comment, ReactionType reactionType) {
        if (reactionType == ReactionType.UPVOTE) {
            comment.incrementUpvoteCount();
            return;
        }

        if (reactionType == ReactionType.DOWNVOTE) {
            comment.incrementDownvoteCount();
        }
    }

    private void decrementReactionCount(Comment comment, ReactionType reactionType) {
        if (reactionType == ReactionType.UPVOTE) {
            comment.decrementUpvoteCount();
            return;
        }

        if (reactionType == ReactionType.DOWNVOTE) {
            comment.decrementDownvoteCount();
        }
    }

    private void validatePostAccessible(Post post, CustomUserDetails currentUser) {
        if (post.getDeletedAt() != null) {
            throw new AppException(PostCode.POST_NOT_FOUND);
        }

        boolean canAccess = post.isPublic()
                || post.getUserId().equals(currentUser.getId())
                || currentUser.getAuthorities().stream()
                        .anyMatch(authority -> authority.getAuthority().equals("post:manage"));

        if (!canAccess) {
            throw new AppException(PostCode.POST_NOT_ACCESSIBLE);
        }
    }
}
