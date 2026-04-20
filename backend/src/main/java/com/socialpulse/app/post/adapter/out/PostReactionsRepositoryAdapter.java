package com.socialpulse.app.post.adapter.out;

import java.util.Optional;

import com.socialpulse.app.post.application.port.out.PostReactionsRepositoryPort;
import com.socialpulse.app.post.domain.model.PostReactions;
import com.socialpulse.app.post.infrastructure.persistence.mapper.PostDomainToEntity;
import com.socialpulse.app.post.infrastructure.persistence.mapper.PostEntityToDomain;
import com.socialpulse.app.post.infrastructure.persistence.repository.JpaPostReactionRepository;

public class PostReactionsRepositoryAdapter implements PostReactionsRepositoryPort {
    private final JpaPostReactionRepository jpaPostReactionRepository;
    private final PostEntityToDomain postEntityToDomain;
    private final PostDomainToEntity postDomainToEntity;

    public PostReactionsRepositoryAdapter(
        JpaPostReactionRepository jpaPostReactionRepository,
        PostEntityToDomain postEntityToDomain,
        PostDomainToEntity postDomainToEntity) {
        this.jpaPostReactionRepository = jpaPostReactionRepository;
        this.postEntityToDomain = postEntityToDomain;
        this.postDomainToEntity = postDomainToEntity;
    }

    @Override
    public Optional<PostReactions> findByPostIdAndUserId(Long postId, Long userId) {
        return jpaPostReactionRepository.findByPostIdAndUserId(postId, userId)
                .map(postEntityToDomain::toDomain);
    }

    @Override
    public PostReactions save(PostReactions postReactions) {
        return postEntityToDomain.toDomain(jpaPostReactionRepository.save(postDomainToEntity.toEntity(postReactions)));
    }

    @Override
    public void delete(PostReactions postReactions) {
        jpaPostReactionRepository.delete(postDomainToEntity.toEntity(postReactions));
    }
}
