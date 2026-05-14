package com.socialpulse.app.feed.adapter.web;

import java.util.List;


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
@Tag(name = "Feed", description = "Personalized feed APIs")
public class FeedController {
    private final GetFeedUseCase getFeedUseCase;
    public FeedController(GetFeedUseCase getFeedUseCase) {
        this.getFeedUseCase = getFeedUseCase;
    }

    @GetMapping
    @PreAuthorize("hasRole('USER') and hasAuthority('VIEW_POST')")
    @Operation(
            summary = "Get personalized feed",
            description = "Get personalized feed for current user with pagination",
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



        return ResponseEntity.ok(
            ApiResponse.<List<FeedItemResponse>>builder()
                .data(feed)
                .build()
        );
    }
}
