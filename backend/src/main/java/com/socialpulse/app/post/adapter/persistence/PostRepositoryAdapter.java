package com.socialpulse.app.post.adapter.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.socialpulse.app.post.domain.enums.PostType;
import com.socialpulse.app.post.domain.enums.Privacy;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.repository.PostRepository;
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
    public List<Post> findByIds(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return jpaPostRepository.findAllByIdIn(ids).stream()
                .map(postPersistenceMapper::toDomain)
                .toList();
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
    public Page<Post> findActiveByUserId(Long userId, Pageable pageable) {
        return jpaPostRepository.findByUserIdAndDeletedAtIsNull(userId, pageable)
                .map(postPersistenceMapper::toDomain);
    }

    @Override
    public Page<Post> findActiveByUserIdAndPrivacy(Long userId, Privacy privacy, Pageable pageable) {
        return jpaPostRepository.findByUserIdAndPrivacyAndDeletedAtIsNull(userId, privacy, pageable)
                .map(postPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByUserIdAndParentPostIdAndType(Long userId, Long parentPostId, PostType type) {
        return jpaPostRepository.existsByUserIdAndParentPostIdAndType(userId, parentPostId, type);
    }

    @Override
    public void updateShareCount(Map<Long, Long> updates) {
        updates.forEach(jpaPostRepository::updateShareCount);
    }

    @Override
    public void deleteById(Long id) {
        jpaPostRepository.deleteById(id);
    }

    @Override
    public long countByUserId(Long userId) {
        return jpaPostRepository.countByUserId(userId);
    }

    @Override
    public Map<Long, Long> countByUserIds(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return jpaPostRepository.countByUserIds(userIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }

    @Override
    public Map<Long, Double> averagePopularityByUserIds(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return jpaPostRepository.averagePopularityByUserIds(userIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Double) row[1]
                ));
    }

    @Override
    public Page<Post> searchPublicActiveByContent(String query, Pageable pageable) {
        return jpaPostRepository.searchPublicActiveByContent(query, pageable)
                .map(postPersistenceMapper::toDomain);
    }

    @Override
    public Page<Post> findPublicActiveByHashtag(String hashtag, Pageable pageable) {
        return jpaPostRepository.findPublicActiveByHashtag(hashtag, pageable)
                .map(postPersistenceMapper::toDomain);
    }

    @Override
    public Page<Post> findPublicActiveByMention(String mention, Pageable pageable) {
        return jpaPostRepository.findPublicActiveByMention(mention, pageable)
                .map(postPersistenceMapper::toDomain);
    }

    @Override
    public List<Post> findRecentPublicActiveSince(LocalDateTime since) {
        return jpaPostRepository.findRecentPublicActiveSince(since).stream()
                .map(postPersistenceMapper::toDomain)
                .toList();
    }
}
