package com.socialpulse.app.feed.domain.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;

import com.socialpulse.app.post.domain.model.Post;

public interface FeedRepository {
    List<Post> findRecentPosts(LocalDateTime since, Pageable pageable);

    List<Post> findFollowingPosts(Long userId, LocalDateTime since, Pageable pageable);

    List<Post> findPopularPosts(LocalDateTime since, Pageable pageable);

    List<Post> findRandomPosts(List<Long> excludeIds, Pageable pageable);

    List<Post> findByTopicSlug(String topicSlug, LocalDateTime since, Pageable pageable);
}
