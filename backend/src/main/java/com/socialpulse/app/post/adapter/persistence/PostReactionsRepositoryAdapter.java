package com.socialpulse.app.post.adapter.persistence;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.socialpulse.app.post.domain.repository.PostReactionsRepository;
import com.socialpulse.app.post.domain.model.PostReactions;
import com.socialpulse.app.post.infrastructure.persistence.mapper.PostPersistenceMapper;
import com.socialpulse.app.post.infrastructure.persistence.repository.JpaPostReactionRepository;

public class PostReactionsRepositoryAdapter implements PostReactionsRepository {
    private final JpaPostReactionRepository jpaPostReactionRepository;
    private final PostPersistenceMapper postPersistenceMapper;

    public PostReactionsRepositoryAdapter(
        JpaPostReactionRepository jpaPostReactionRepository,
        PostPersistenceMapper postPersistenceMapper) {
        this.jpaPostReactionRepository = jpaPostReactionRepository;
        this.postPersistenceMapper = postPersistenceMapper;
    }

    @Override
    public Optional<PostReactions> findByPostIdAndUserId(Long postId, Long userId) {
        return jpaPostReactionRepository.findByPostIdAndUserId(postId, userId)
                .map(postPersistenceMapper::toDomain);
    }

    @Override
    public List<PostReactions> findByUserIdAndPostIds(Long userId, Set<Long> postIds) {
        if (userId == null || postIds == null || postIds.isEmpty()) {
            return List.of();
        }
        return jpaPostReactionRepository.findByUserIdAndPostIdIn(userId, postIds).stream()
                .map(postPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public PostReactions save(PostReactions postReactions) {
        return postPersistenceMapper.toDomain(jpaPostReactionRepository.save(postPersistenceMapper.toEntity(postReactions)));
    }

    @Override
    public void delete(PostReactions postReactions) {
        jpaPostReactionRepository.delete(postPersistenceMapper.toEntity(postReactions));
    }
}

