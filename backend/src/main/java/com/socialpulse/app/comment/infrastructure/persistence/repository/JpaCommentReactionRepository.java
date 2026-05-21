package com.socialpulse.app.comment.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.socialpulse.app.comment.infrastructure.persistence.entity.CommentReactionEntity;

@Repository
public interface JpaCommentReactionRepository extends JpaRepository<CommentReactionEntity, Long> {
    Optional<CommentReactionEntity> findByCommentIdAndUserId(Long commentId, Long userId);

    List<CommentReactionEntity> findByUserIdAndCommentIdIn(Long userId, Collection<Long> commentIds);
}
