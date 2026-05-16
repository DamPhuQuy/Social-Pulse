package com.socialpulse.app.follow.adapter.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.socialpulse.app.common.dto.response.ApiResponse;
import com.socialpulse.app.follow.application.dto.response.FollowResponse;
import com.socialpulse.app.follow.application.dto.response.FollowersListResponse;
import com.socialpulse.app.follow.application.dto.response.FollowingListResponse;
import com.socialpulse.app.follow.application.usecase.FollowUserUseCase;
import com.socialpulse.app.follow.application.usecase.GetFollowersUseCase;
import com.socialpulse.app.follow.application.usecase.GetFollowingUseCase;
import com.socialpulse.app.follow.application.usecase.UnfollowUserUseCase;
import com.socialpulse.app.security.user.CustomUserDetails;

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

    public FollowController(FollowUserUseCase followUserUseCase,
                           UnfollowUserUseCase unfollowUserUseCase,
                           GetFollowersUseCase getFollowersUseCase,
                           GetFollowingUseCase getFollowingUseCase) {
        this.followUserUseCase = followUserUseCase;
        this.unfollowUserUseCase = unfollowUserUseCase;
        this.getFollowersUseCase = getFollowersUseCase;
        this.getFollowingUseCase = getFollowingUseCase;
    }

    @PostMapping("/{userId}")
    @PreAuthorize("hasRole('USER')")
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
    @PreAuthorize("hasRole('USER')")
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
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Get user's followers",
        description = "Get list of users who follow the specified user",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved followers list"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "User not found"
            )
        }
    )
    public ResponseEntity<ApiResponse<FollowersListResponse>> getFollowers(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        FollowersListResponse response = getFollowersUseCase.getFollowers(userId, currentUser.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.<FollowersListResponse>builder()
                .data(response)
                .message("Successfully retrieved followers")
                .build());
    }

    @GetMapping("/{userId}/following")
    @PreAuthorize("hasRole('USER')")
    @Operation(
        summary = "Get user's following",
        description = "Get list of users that the specified user is following",
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved following list"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "User not found"
            )
        }
    )
    public ResponseEntity<ApiResponse<FollowingListResponse>> getFollowing(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        FollowingListResponse response = getFollowingUseCase.getFollowing(userId, currentUser.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.<FollowingListResponse>builder()
                .data(response)
                .message("Successfully retrieved following")
                .build());
    }
}
