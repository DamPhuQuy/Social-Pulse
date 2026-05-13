package com.socialpulse.app.behavior.adapter.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.socialpulse.app.behavior.domain.enums.EventType;
import com.socialpulse.app.behavior.domain.model.UserBehavior;
import com.socialpulse.app.behavior.domain.repository.UserBehaviorRepository;
import com.socialpulse.app.behavior.infrastructure.persistence.entity.UserBehaviorEntity;
import com.socialpulse.app.behavior.infrastructure.persistence.repository.UserBehaviorJpaRepository;

public class UserBehaviorRepositoryAdapter implements UserBehaviorRepository {
    private final UserBehaviorJpaRepository jpaRepository;

    public UserBehaviorRepositoryAdapter(UserBehaviorJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public UserBehavior save(UserBehavior behavior) {
        UserBehaviorEntity entity = toEntity(behavior);
        UserBehaviorEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<UserBehavior> findByUserIdSince(Long userId, LocalDateTime since) {
        return jpaRepository.findByUserIdAndEventTimeSince(userId, since)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserBehavior> findByPostIdSince(Long postId, LocalDateTime since) {
        return jpaRepository.findByPostIdAndEventTimeSince(postId, since)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserBehavior> findByUserIdAndPostIdsSince(Long userId, List<Long> postIds, LocalDateTime since) {
        return jpaRepository.findByUserIdAndPostIdsAndEventTimeSince(userId, postIds, since)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Long countInteractions(Long userId, Long postId, List<EventType> eventTypes, LocalDateTime since) {
        return jpaRepository.countInteractions(userId, postId, eventTypes, since);
    }

    @Override
    public Map<Long, Long> countInteractionsByPost(Long userId, List<EventType> eventTypes, LocalDateTime since) {
        List<Object[]> results = jpaRepository.countInteractionsByPost(userId, eventTypes, since);
        return results.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }

    private UserBehaviorEntity toEntity(UserBehavior behavior) {
        return UserBehaviorEntity.builder()
                .id(behavior.getId())
                .userId(behavior.getUserId())
                .postId(behavior.getPostId())
                .eventType(behavior.getEventType())
                .eventTime(behavior.getEventTime())
                .position(behavior.getPosition())
                .dwellTimeSeconds(behavior.getDwellTimeSeconds())
                .metadata(behavior.getMetadata())
                .build();
    }

    private UserBehavior toDomain(UserBehaviorEntity entity) {
        return UserBehavior.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .postId(entity.getPostId())
                .eventType(entity.getEventType())
                .eventTime(entity.getEventTime())
                .position(entity.getPosition())
                .dwellTimeSeconds(entity.getDwellTimeSeconds())
                .metadata(entity.getMetadata())
                .build();
    }
}

