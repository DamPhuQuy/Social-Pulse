package com.socialpulse.app.behavior.adapter.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.socialpulse.app.behavior.application.dto.TrackBehaviorRequest;
import com.socialpulse.app.behavior.application.usecase.BehaviorTrackingUseCase;
import com.socialpulse.app.behavior.domain.model.UserBehavior;

@RestController
@RequestMapping("/api/v1/behaviors")
public class BehaviorTrackingController {
    private final BehaviorTrackingUseCase trackBehaviorUseCase;

    public BehaviorTrackingController(BehaviorTrackingUseCase trackBehaviorUseCase) {
        this.trackBehaviorUseCase = trackBehaviorUseCase;
    }

    @PostMapping("/track")
    public ResponseEntity<Void> trackBehavior(@RequestBody TrackBehaviorRequest request) {
        UserBehavior behavior = UserBehavior.builder()
                .userId(request.getUserId())
                .postId(request.getPostId())
                .eventType(request.getEventType())
                .position(request.getPosition())
                .dwellTimeSeconds(request.getDwellTimeSeconds())
                .metadata(request.getMetadata())
                .build();

        trackBehaviorUseCase.trackBehavior(behavior);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/batch")
    public ResponseEntity<Void> trackBehaviorBatch(@RequestBody TrackBehaviorRequest[] requests) {
        for (TrackBehaviorRequest request : requests) {
            UserBehavior behavior = UserBehavior.builder()
                    .userId(request.getUserId())
                    .postId(request.getPostId())
                    .eventType(request.getEventType())
                    .position(request.getPosition())
                    .dwellTimeSeconds(request.getDwellTimeSeconds())
                    .metadata(request.getMetadata())
                    .build();

            trackBehaviorUseCase.trackBehavior(behavior);
        }
        return ResponseEntity.accepted().build();
    }
}
