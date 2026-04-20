package com.socialpulse.app.post.application.port.out;

import java.util.Optional;

import com.socialpulse.app.post.domain.model.PostReactions;

public interface PostReactionsRepositoryPort {
    Optional<PostReactions> findByPostIdAndUserId(Long postId, Long userId);

    PostReactions save(PostReactions postReactions);

    void delete(PostReactions postReactions);
}
