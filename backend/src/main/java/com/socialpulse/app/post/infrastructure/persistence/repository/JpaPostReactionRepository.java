package com.socialpulse.app.post.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.socialpulse.app.post.infrastructure.persistence.entity.PostReactionsEntity;

@Repository
public interface JpaPostReactionRepository extends JpaRepository<PostReactionsEntity, Long> {
    Optional<PostReactionsEntity> findByPostIdAndUserId(Long postId, Long userId);

    List<PostReactionsEntity> findByUserIdAndPostIdIn(Long userId, Collection<Long> postIds);
}
