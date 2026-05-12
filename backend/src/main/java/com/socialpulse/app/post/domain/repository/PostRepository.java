package com.socialpulse.app.post.domain.repository;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.socialpulse.app.post.domain.enums.PostType;
import com.socialpulse.app.post.domain.model.Post;

public interface PostRepository {
    Optional<Post> findById(Long id);

    Post save(Post post);

    Page<Post> findByUserId(Long userId, Pageable pageable);

    boolean existsByUserIdAndParentPostIdAndType(Long userId, Long parentPostId, PostType type);

    void updateShareCount(Map<Long, Long> updates);

    void deleteById(Long id);

    long countByUserId(Long userId);

    Map<Long, Long> countByUserIds(Set<Long> userIds);
}


