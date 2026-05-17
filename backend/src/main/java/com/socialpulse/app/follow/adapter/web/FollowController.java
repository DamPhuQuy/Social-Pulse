package com.socialpulse.app.follow.adapter.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.socialpulse.app.common.dto.response.PageResponse;
import com.socialpulse.app.common.dto.response.ApiResponse;
import com.socialpulse.app.follow.application.dto.response.FollowCountsResponse;
import com.socialpulse.app.follow.application.dto.response.FollowResponse;
import com.socialpulse.app.follow.application.dto.response.FollowStatusResponse;
import com.socialpulse.app.follow.application.usecase.FollowUserUseCase;
import com.socialpulse.app.follow.application.usecase.GetFollowCountsUseCase;
import com.socialpulse.app.follow.application.usecase.GetFollowersUseCase;
import com.socialpulse.app.follow.application.usecase.GetFollowStatusUseCase;
import com.socialpulse.app.follow.application.usecase.GetFollowingUseCase;
import com.socialpulse.app.follow.application.usecase.UnfollowUserUseCase;
import com.socialpulse.app.security.user.CustomUserDetails;
import com.socialpulse.app.user.application.dto.response.UserSummary;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/follows")
@Tag(name = "Follows", description = "Follow management APIs")
public class FollowController {

    private final FollowUserUseCase followUserUseCase;
    private final UnfollowUserUseCase unfollowUserUseCase;
    private final GetFollowersUseCase getFollowersUseCase;
    private final GetFollowingUseCase getFollowingUseCase;
    private final GetFollowStatusUseCase getFollowStatusUseCase;
    private final GetFollowCountsUseCase getFollowCountsUseCase;

    public FollowController(FollowUserUseCase followUserUseCase,
                           UnfollowUserUseCase unfollowUserUseCase,
                           GetFollowersUseCase getFollowersUseCase,
                           GetFollowingUseCase getFollowingUseCase,
                           GetFollowStatusUseCase getFollowStatusUseCase,
                           GetFollowCountsUseCase getFollowCountsUseCase) {
        this.followUserUseCase = followUserUseCase;
        this.unfollowUserUseCase = unfollowUserUseCase;
        this.getFollowersUseCase = getFollowersUseCase;
        this.getFollowingUseCase = getFollowingUseCase;
        this.getFollowStatusUseCase = getFollowStatusUseCase;
        this.getFollowCountsUseCase = getFollowCountsUseCase;
    }

    @PostMapping("/{userId}")
    @PreAuthorize("hasAuthority('follow:create')")
    @Operation(
        summary = "Follow a user",
        description = "Follow another user by their ID",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "Successfully followed user"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Cannot follow yourself or already following"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "User not found"
            )
        }
    )
    public ResponseEntity<ApiResponse<FollowResponse>> followUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        FollowResponse response = followUserUseCase.followUser(userId, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<FollowResponse>builder()
                        .data(response)
                        .message("Successfully followed user")
                        .build());
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAuthority('follow:delete')")
    @Operation(
        summary = "Unfollow a user",
        description = "Unfollow a user by their ID",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully unfollowed user"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Not following this user"
            )
        }
    )
    public ResponseEntity<ApiResponse<Void>> unfollowUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        unfollowUserUseCase.unfollowUser(userId, currentUser);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Successfully unfollowed user")
                .build());
    }

    @GetMapping("/{userId}/followers")
    @PreAuthorize("hasAuthority('follow:read')")
    @Operation(summary = "Get followers", description = "Get paginated followers of a user")
    public ResponseEntity<ApiResponse<PageResponse<UserSummary>>> getFollowers(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<UserSummary>>builder()
                .data(getFollowersUseCase.getFollowers(userId, page, size))
                .build());
    }

    @GetMapping("/{userId}/following")
    @PreAuthorize("hasAuthority('follow:read')")
    @Operation(summary = "Get following", description = "Get paginated users a user is following")
    public ResponseEntity<ApiResponse<PageResponse<UserSummary>>> getFollowing(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.<PageResponse<UserSummary>>builder()
                .data(getFollowingUseCase.getFollowing(userId, page, size))
                .build());
    }

    @GetMapping("/{userId}/status")
    @PreAuthorize("hasAuthority('follow:read')")
    @Operation(summary = "Get follow status", description = "Get whether current user follows the target user")
    public ResponseEntity<ApiResponse<FollowStatusResponse>> getFollowStatus(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.<FollowStatusResponse>builder()
                .data(getFollowStatusUseCase.getFollowStatus(userId, currentUser.getId()))
                .build());
    }

    @GetMapping("/{userId}/counts")
    @PreAuthorize("hasAuthority('follow:read')")
    @Operation(summary = "Get follow counts", description = "Get follower and following counts of a user")
    public ResponseEntity<ApiResponse<FollowCountsResponse>> getFollowCounts(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.<FollowCountsResponse>builder()
                .data(getFollowCountsUseCase.getFollowCounts(userId))
                .build());
    }
}
