package com.socialpulse.app.behavior.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.socialpulse.app.behavior.domain.enums.EventType;
import com.socialpulse.app.behavior.domain.model.UserBehavior;

public interface UserBehaviorRepository {
    UserBehavior save(UserBehavior behavior);

    List<UserBehavior> findByUserIdSince(Long userId, LocalDateTime since);

    List<UserBehavior> findByPostIdSince(Long postId, LocalDateTime since);

    List<UserBehavior> findByUserIdAndPostIdsSince(Long userId, List<Long> postIds, LocalDateTime since);

    Long countInteractions(Long userId, Long postId, List<EventType> eventTypes, LocalDateTime since);

    Map<Long, Long> countInteractionsByPost(Long userId, List<EventType> eventTypes, LocalDateTime since);
}

