package com.socialpulse.app.follow.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.socialpulse.app.follow.domain.model.Follow;

public interface FollowRepository {
    Follow save(Follow follow);

    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    long countByFollowerId(Long followerId);

    long countByFollowingId(Long followingId);

    Set<Long> findFollowedUserIds(Long followerId, Set<Long> candidateFolloweeIds);

    List<Follow> findFollowersByUserId(Long userId, int offset, int limit);

    List<Follow> findFollowingByUserId(Long userId, int offset, int limit);
}

