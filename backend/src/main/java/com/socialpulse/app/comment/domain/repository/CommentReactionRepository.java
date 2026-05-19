package com.socialpulse.app.comment.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.socialpulse.app.comment.domain.model.CommentReaction;

public interface CommentReactionRepository {
    Optional<CommentReaction> findByCommentIdAndUserId(Long commentId, Long userId);

    List<CommentReaction> findByUserIdAndCommentIds(Long userId, Set<Long> commentIds);

    CommentReaction save(CommentReaction commentReaction);

    void delete(CommentReaction commentReaction);
}
