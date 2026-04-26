package com.socialpulse.app.follow.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.socialpulse.app.follow.infrastructure.persistence.entity.FollowEntity;

@Repository
public interface JpaFollowRepository extends JpaRepository<FollowEntity, Long> {
    Optional<FollowEntity> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    long countByFollowerId(Long followerId);

    long countByFollowingId(Long followingId);
}
