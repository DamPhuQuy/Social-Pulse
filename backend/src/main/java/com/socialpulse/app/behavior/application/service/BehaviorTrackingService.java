package com.socialpulse.app.behavior.application.service;

import com.socialpulse.app.behavior.application.usecase.TrackBehaviorUseCase;
import com.socialpulse.app.behavior.domain.model.UserBehavior;
import com.socialpulse.app.behavior.domain.repository.UserBehaviorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class BehaviorTrackingService implements TrackBehaviorUseCase {
    private final UserBehaviorRepository behaviorRepository;

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
