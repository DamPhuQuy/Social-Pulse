package com.socialpulse.app.feed.adapter.web;

import java.util.List;




import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import com.socialpulse.app.common.dto.response.ApiResponse;
import com.socialpulse.app.feed.application.dto.response.FeedItemResponse;
import com.socialpulse.app.feed.application.usecase.GetFeedUseCase;
import com.socialpulse.app.security.user.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;

@RestController
@RequestMapping("/api/v1/feed")
@Tag(name = "Feed", description = "Personalized feed APIs")
@Validated
public class FeedController {
    private final GetFeedUseCase getFeedUseCase;
    public FeedController(GetFeedUseCase getFeedUseCase) {
        this.getFeedUseCase = getFeedUseCase;
    }

    @GetMapping
    @Operation(summary = "Get feed", description = "Get personalized home feed or topic feed when topicSlug is provided")
    public ResponseEntity<ApiResponse<List<FeedItemResponse>>> getFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") @Max(100) int size,
            @RequestParam(required = false) String topicSlug,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        List<FeedItemResponse> feed = topicSlug != null
                ? getFeedUseCase.getFeed(page, size, topicSlug, currentUser)
                : getFeedUseCase.getFeed(page, size, currentUser);

        return ResponseEntity.ok(ApiResponse.<List<FeedItemResponse>>builder().data(feed).build());
    }
}
