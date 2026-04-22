package com.socialpulse.app.post.adapter.persistence;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.socialpulse.app.post.domain.enums.PostType;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.infrastructure.persistence.mapper.PostPersistenceMapper;
import com.socialpulse.app.post.infrastructure.persistence.repository.JpaPostRepository;

public class PostRepositoryAdapter implements PostRepository {
    private final JpaPostRepository jpaPostRepository;
    private final PostPersistenceMapper postPersistenceMapper;

    public PostRepositoryAdapter(JpaPostRepository jpaPostRepository,
                                 PostPersistenceMapper postPersistenceMapper) {
        this.jpaPostRepository = jpaPostRepository;
        this.postPersistenceMapper = postPersistenceMapper;
    }

    @Override
    public Optional<Post> findById(Long id) {
        return jpaPostRepository.findById(id).map(postPersistenceMapper::toDomain);
    }

    @Override
    public Post save(Post post) {
        return postPersistenceMapper.toDomain(jpaPostRepository.save(postPersistenceMapper.toEntity(post)));
    }

    @Override
    public Page<Post> findByUserId(Long userId, Pageable pageable) {
        return jpaPostRepository.findByUserId(userId, pageable).map(postPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByUserIdAndParentPostIdAndType(Long userId, Long parentPostId, PostType type) {
        return jpaPostRepository.existsByUserIdAndParentPostIdAndType(userId, parentPostId, type);
    }

}


