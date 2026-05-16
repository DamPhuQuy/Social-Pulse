package com.socialpulse.app.comment.domain.repository;

import java.util.Optional;

import com.socialpulse.app.comment.domain.model.CommentReaction;

public interface CommentReactionRepository {
    Optional<CommentReaction> findByCommentIdAndUserId(Long commentId, Long userId);

    CommentReaction save(CommentReaction commentReaction);

    void delete(CommentReaction commentReaction);
}
