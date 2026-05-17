package com.socialpulse.app.follow.domain.repository;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.socialpulse.app.follow.domain.model.Follow;

public interface FollowRepository {
    Follow save(Follow follow);

    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    long countByFollowerId(Long followerId);

    long countByFollowingId(Long followingId);

    Set<Long> findFollowedUserIds(Long followerId, Set<Long> candidateFolloweeIds);

    Page<Long> findFollowerIdsByFollowingId(Long followingId, Pageable pageable);

    Page<Long> findFollowingIdsByFollowerId(Long followerId, Pageable pageable);
}
