package com.socialpulse.app.post.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.socialpulse.app.post.entity.PostReactions;

@Repository
public interface PostReactionRepository extends JpaRepository<PostReactions, Long> {
    Optional<PostReactions> findByPostIdAndUserId(Long postId, Long userId);
}
