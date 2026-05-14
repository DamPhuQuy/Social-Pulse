package com.socialpulse.app.follow.adapter.persistence;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.follow.domain.model.Follow;
import com.socialpulse.app.follow.domain.repository.FollowRepository;
import com.socialpulse.app.follow.infrastructure.persistence.entity.FollowEntity;
import com.socialpulse.app.follow.infrastructure.persistence.mapper.FollowPersistenceMapper;
import com.socialpulse.app.follow.infrastructure.persistence.repository.JpaFollowRepository;
import com.socialpulse.app.user.infrastructure.persistence.entity.UserEntity;
import com.socialpulse.app.user.infrastructure.persistence.repository.JpaUserRepository;

public class FollowRepositoryAdapter implements FollowRepository {

    private final JpaFollowRepository jpaFollowRepository;
    private final JpaUserRepository jpaUserRepository;
    private final FollowPersistenceMapper mapper;

    public FollowRepositoryAdapter(JpaFollowRepository jpaFollowRepository,
                                   JpaUserRepository jpaUserRepository,
                                   FollowPersistenceMapper mapper) {
        this.jpaFollowRepository = jpaFollowRepository;
        this.jpaUserRepository = jpaUserRepository;
        this.mapper = mapper;
    }

    @Override
    public Follow save(Follow follow) {
        UserEntity follower = jpaUserRepository.findById(follow.getFollowerId())
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));
        UserEntity following = jpaUserRepository.findById(follow.getFollowingId())
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        FollowEntity entity = mapper.toEntity(follow, follower, following);
        FollowEntity saved = jpaFollowRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId) {
        return jpaFollowRepository.findByFollowerIdAndFollowingId(followerId, followingId)
                .map(mapper::toDomain);
    }

    @Override
    public void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId) {
        jpaFollowRepository.deleteByFollowerIdAndFollowingId(followerId, followingId);
    }

    @Override
    public boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId) {
        return jpaFollowRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    @Override
    public long countByFollowerId(Long followerId) {
        return jpaFollowRepository.countByFollowerId(followerId);
    }

    @Override
    public long countByFollowingId(Long followingId) {
        return jpaFollowRepository.countByFollowingId(followingId);
    }

    @Override
    public Set<Long> findFollowedUserIds(Long followerId, Set<Long> candidateFolloweeIds) {
        if (candidateFolloweeIds == null || candidateFolloweeIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(jpaFollowRepository.findFollowingIdsByFollowerIdAndFollowingIdIn(
                followerId, candidateFolloweeIds));
    }
}

