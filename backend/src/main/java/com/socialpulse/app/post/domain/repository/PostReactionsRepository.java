package com.socialpulse.app.post.domain.repository;

import java.util.Optional;

import com.socialpulse.app.post.domain.model.PostReactions;

public interface PostReactionsRepository {
    Optional<PostReactions> findByPostIdAndUserId(Long postId, Long userId);

    PostReactions save(PostReactions postReactions);

    void delete(PostReactions postReactions);
}

