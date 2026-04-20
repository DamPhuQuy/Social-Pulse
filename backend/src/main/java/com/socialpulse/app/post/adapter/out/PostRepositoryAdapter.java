package com.socialpulse.app.post.adapter.out;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.socialpulse.app.post.application.port.out.PostRepositoryPort;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.infrastructure.persistence.mapper.PostDomainToEntity;
import com.socialpulse.app.post.infrastructure.persistence.mapper.PostEntityToDomain;
import com.socialpulse.app.post.infrastructure.persistence.repository.JpaPostRepository;

public class PostRepositoryAdapter implements PostRepositoryPort {
    private final JpaPostRepository jpaPostRepository;
    private final PostEntityToDomain postEntityToDomain;
    private final PostDomainToEntity postDomainToEntity;

    public PostRepositoryAdapter(JpaPostRepository jpaPostRepository,
                                 PostEntityToDomain postEntityToDomain,
                                 PostDomainToEntity postDomainToEntity) {
        this.jpaPostRepository = jpaPostRepository;
        this.postEntityToDomain = postEntityToDomain;
        this.postDomainToEntity = postDomainToEntity;
    }

    @Override
    public Optional<Post> findById(Long id) {
        return jpaPostRepository.findById(id).map(postEntityToDomain::toDomain);
    }

    @Override
    public Post save(Post post) {
        return postEntityToDomain.toDomain(jpaPostRepository.save(postDomainToEntity.toEntity(post)));
    }

    @Override
    public Page<Post> findByUserId(Long userId, Pageable pageable) {
        return jpaPostRepository.findByUserId(userId, pageable).map(postEntityToDomain::toDomain);
    }

}
