package com.socialpulse.app.behavior.application.service;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.behavior.application.usecase.BehaviorTrackingUseCase;
import com.socialpulse.app.behavior.domain.model.UserBehavior;
import com.socialpulse.app.behavior.domain.repository.UserBehaviorRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BehaviorTrackingService implements BehaviorTrackingUseCase {
    private final UserBehaviorRepository behaviorRepository;

    public BehaviorTrackingService(UserBehaviorRepository behaviorRepository) {
        this.behaviorRepository = behaviorRepository;
    }

    @Override
    @Async
    @Transactional
    public UserBehavior trackBehavior(UserBehavior behavior) {
        try {
            if (behavior.getEventTime() == null) {
                behavior = UserBehavior.builder()
                        .userId(behavior.getUserId())
                        .postId(behavior.getPostId())
                        .eventType(behavior.getEventType())
                        .eventTime(LocalDateTime.now())
                        .position(behavior.getPosition())
                        .dwellTimeSeconds(behavior.getDwellTimeSeconds())
                        .metadata(behavior.getMetadata())
                        .build();
            }

            UserBehavior saved = behaviorRepository.save(behavior);
            log.debug("Tracked behavior: userId={}, postId={}, eventType={}",
                    saved.getUserId(), saved.getPostId(), saved.getEventType());
            return saved;
        } catch (Exception e) {
            log.error("Failed to track behavior: userId={}, postId={}, eventType={}",
                    behavior.getUserId(), behavior.getPostId(), behavior.getEventType(), e);
            throw e;
        }
    }
}
