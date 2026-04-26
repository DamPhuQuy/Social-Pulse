package com.socialpulse.app.behavior.adapter.web;

import com.socialpulse.app.behavior.application.dto.TrackBehaviorRequest;
import com.socialpulse.app.behavior.application.usecase.TrackBehaviorUseCase;
import com.socialpulse.app.behavior.domain.model.UserBehavior;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/behaviors")
@RequiredArgsConstructor
public class BehaviorTrackingController {
    private final TrackBehaviorUseCase trackBehaviorUseCase;

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
