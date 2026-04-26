package com.socialpulse.app.feed.adapter.web;

import java.util.List;

import com.socialpulse.app.behavior.application.usecase.BehaviorTrackingUseCase;
import com.socialpulse.app.behavior.domain.enums.EventType;
import com.socialpulse.app.behavior.domain.model.UserBehavior;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.socialpulse.app.common.dto.response.ApiResponse;
import com.socialpulse.app.feed.application.dto.response.FeedItemResponse;
import com.socialpulse.app.feed.application.usecase.GetFeedUseCase;
import com.socialpulse.app.security.user.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/feed")
@Tag(name = "Feed", description = "AI-powered feed ranking APIs")
public class FeedController {
    private final GetFeedUseCase getFeedUseCase;
    private final BehaviorTrackingUseCase trackBehaviorUseCase;

    public FeedController(GetFeedUseCase getFeedUseCase, BehaviorTrackingUseCase trackBehaviorUseCase) {
        this.getFeedUseCase = getFeedUseCase;
        this.trackBehaviorUseCase = trackBehaviorUseCase;
    }

    @GetMapping
    @PreAuthorize("hasRole('USER') and hasAuthority('VIEW_POST')")
    @Operation(
            summary = "Get personalized feed",
            description = "Get AI-ranked personalized feed for current user with pagination",
            responses = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Feed retrieved successfully"
                ),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
                )
            }
    )
    public ResponseEntity<ApiResponse<List<FeedItemResponse>>> getFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        List<FeedItemResponse> feed = getFeedUseCase.getFeed(page, size, currentUser);

        // Track impressions for all posts in the feed
        int position = page * size;
        for (FeedItemResponse item : feed) {
            UserBehavior impression = UserBehavior.builder()
                    .userId(currentUser.getId())
                    .postId(item.getPostId())
                    .eventType(EventType.IMPRESSION)
                    .position(position++)
                    .build();
            trackBehaviorUseCase.trackBehavior(impression);
        }

        return ResponseEntity.ok(
            ApiResponse.<List<FeedItemResponse>>builder()
                .data(feed)
                .build()
        );
    }
}
