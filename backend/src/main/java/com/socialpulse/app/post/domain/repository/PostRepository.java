package com.socialpulse.app.post.domain.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.socialpulse.app.post.domain.enums.PostType;
import com.socialpulse.app.post.domain.enums.Privacy;
import com.socialpulse.app.post.domain.model.Post;

public interface PostRepository {
    Optional<Post> findById(Long id);

    List<Post> findByIds(Set<Long> ids);

    Post save(Post post);

    Page<Post> findByUserId(Long userId, Pageable pageable);

    Page<Post> findActiveByUserId(Long userId, Pageable pageable);

    Page<Post> findActiveByUserIdAndPrivacy(Long userId, Privacy privacy, Pageable pageable);

    boolean existsByUserIdAndParentPostIdAndType(Long userId, Long parentPostId, PostType type);

    void updateShareCount(Map<Long, Long> updates);

    void deleteById(Long id);

    long countByUserId(Long userId);

    Map<Long, Long> countByUserIds(Set<Long> userIds);

    Map<Long, Double> averagePopularityByUserIds(Set<Long> userIds);

    Page<Post> searchPublicActiveByContent(String query, Pageable pageable);

    Page<Post> findPublicActiveByHashtag(String hashtag, Pageable pageable);

    Page<Post> findPublicActiveByMention(String mention, Pageable pageable);

    List<Post> findRecentPublicActiveSince(LocalDateTime since);
}
