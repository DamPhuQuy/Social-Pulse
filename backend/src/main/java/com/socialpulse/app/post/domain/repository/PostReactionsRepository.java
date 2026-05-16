package com.socialpulse.app.post.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.socialpulse.app.post.domain.model.PostReactions;

public interface PostReactionsRepository {
    Optional<PostReactions> findByPostIdAndUserId(Long postId, Long userId);

    List<PostReactions> findByUserIdAndPostIds(Long userId, Set<Long> postIds);

    PostReactions save(PostReactions postReactions);

    void delete(PostReactions postReactions);
}
