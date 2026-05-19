package com.socialpulse.app.comment.adapter.persistence;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.socialpulse.app.comment.domain.model.CommentReaction;
import com.socialpulse.app.comment.domain.repository.CommentReactionRepository;
import com.socialpulse.app.comment.infrastructure.persistence.mapper.CommentPersistenceMapper;
import com.socialpulse.app.comment.infrastructure.persistence.repository.JpaCommentReactionRepository;

public class CommentReactionRepositoryAdapter implements CommentReactionRepository {
    private final JpaCommentReactionRepository jpaCommentReactionRepository;
    private final CommentPersistenceMapper commentPersistenceMapper;

    public CommentReactionRepositoryAdapter(
            JpaCommentReactionRepository jpaCommentReactionRepository,
            CommentPersistenceMapper commentPersistenceMapper) {
        this.jpaCommentReactionRepository = jpaCommentReactionRepository;
        this.commentPersistenceMapper = commentPersistenceMapper;
    }

    @Override
    public Optional<CommentReaction> findByCommentIdAndUserId(Long commentId, Long userId) {
        return jpaCommentReactionRepository.findByCommentIdAndUserId(commentId, userId)
                .map(commentPersistenceMapper::toDomain);
    }

    @Override
    public List<CommentReaction> findByUserIdAndCommentIds(Long userId, Set<Long> commentIds) {
        if (userId == null || commentIds == null || commentIds.isEmpty()) {
            return List.of();
        }
        return jpaCommentReactionRepository.findByUserIdAndCommentIdIn(userId, commentIds).stream()
                .map(commentPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public CommentReaction save(CommentReaction commentReaction) {
        return commentPersistenceMapper.toDomain(
                jpaCommentReactionRepository.save(commentPersistenceMapper.toEntity(commentReaction)));
    }

    @Override
    public void delete(CommentReaction commentReaction) {
        jpaCommentReactionRepository.delete(commentPersistenceMapper.toEntity(commentReaction));
    }
}
