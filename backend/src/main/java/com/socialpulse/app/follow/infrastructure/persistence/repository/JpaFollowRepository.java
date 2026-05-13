package com.socialpulse.app.follow.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.socialpulse.app.follow.infrastructure.persistence.entity.FollowEntity;

@Repository
public interface JpaFollowRepository extends JpaRepository<FollowEntity, Long> {
    Optional<FollowEntity> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    long countByFollowerId(Long followerId);

    long countByFollowingId(Long followingId);

    @Query("SELECT f.following.id FROM FollowEntity f WHERE f.follower.id = :followerId AND f.following.id IN :followingIds")
    List<Long> findFollowingIdsByFollowerIdAndFollowingIdIn(
            @Param("followerId") Long followerId,
            @Param("followingIds") Set<Long> followingIds);
}

