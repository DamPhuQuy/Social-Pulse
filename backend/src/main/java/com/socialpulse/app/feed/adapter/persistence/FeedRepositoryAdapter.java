package com.socialpulse.app.feed.adapter.persistence;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.socialpulse.app.feed.domain.repository.FeedRepository;
import com.socialpulse.app.feed.infrastructure.persistence.repository.JpaFeedPostRepository;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.infrastructure.persistence.entity.PostEntity;
import com.socialpulse.app.post.infrastructure.persistence.mapper.PostPersistenceMapper;

@Repository
public class FeedRepositoryAdapter implements FeedRepository {
    private final JpaFeedPostRepository jpaFeedPostRepository;
    private final PostPersistenceMapper postPersistenceMapper;

    public FeedRepositoryAdapter(JpaFeedPostRepository jpaFeedPostRepository,
                                 PostPersistenceMapper postPersistenceMapper) {
        this.jpaFeedPostRepository = jpaFeedPostRepository;
        this.postPersistenceMapper = postPersistenceMapper;
    }

    @Override
    public List<Post> findRecentPosts(LocalDateTime since, Pageable pageable) {
        List<PostEntity> entities = jpaFeedPostRepository.findRecentPosts(since, pageable);
        return entities.stream().map(postPersistenceMapper::toDomain).toList();
    }

    @Override
    public List<Post> findFollowingPosts(Long userId, LocalDateTime since, Pageable pageable) {
        List<PostEntity> entities = jpaFeedPostRepository.findFollowingPosts(userId, since, pageable);
        return entities.stream().map(postPersistenceMapper::toDomain).toList();
    }

    @Override
    public List<Post> findFollowingUserAndTopicPosts(Long userId, LocalDateTime since, Pageable pageable) {
        List<PostEntity> entities = jpaFeedPostRepository.findFollowingUserAndTopicPosts(userId, since, pageable);
        return entities.stream().map(postPersistenceMapper::toDomain).toList();
    }

    @Override
    public List<Post> findPopularPosts(LocalDateTime since, Pageable pageable) {
        List<PostEntity> entities = jpaFeedPostRepository.findPopularPosts(since, pageable);
        return entities.stream().map(postPersistenceMapper::toDomain).toList();
    }

    @Override
    public List<Post> findByTopicSlug(String topicSlug, LocalDateTime since, Pageable pageable) {
        String normalized = topicSlug.trim().toLowerCase();
        List<PostEntity> entities = jpaFeedPostRepository.findByTopicSlug(normalized, since, pageable);
        return entities.stream().map(postPersistenceMapper::toDomain).toList();
    }
}
