package com.socialpulse.app.user.adapter.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.socialpulse.app.common.dto.response.ApiResponse;
import com.socialpulse.app.security.user.CustomUserDetails;
import com.socialpulse.app.user.application.dto.request.UserViewProfileRequest;
import com.socialpulse.app.user.application.dto.response.UserViewProfileResponse;
import com.socialpulse.app.user.application.usecase.GetUserProfileUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User profile APIs")
public class UserController {

    private final GetUserProfileUseCase getUserProfileUseCase;

    public UserController(GetUserProfileUseCase getUserProfileUseCase) {
        this.getUserProfileUseCase = getUserProfileUseCase;
    }

    @GetMapping("/profile")
    @Operation(
            summary = "Get my profile",
            description = "Return profile of current authenticated user",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
            }
    )
    public ResponseEntity<ApiResponse<UserViewProfileResponse>> getProfile(@AuthenticationPrincipal CustomUserDetails currentUser) {
        var request = UserViewProfileRequest.builder()
                .targetUserId(currentUser.getId())
                .build();

        return ResponseEntity.ok(ApiResponse.<UserViewProfileResponse>builder().data(getUserProfileUseCase.getProfile(request)).build());
    }

    @GetMapping("/profile/{username}")
    @PreAuthorize("hasAuthority('user:read')")
    @Operation(
            summary = "Get user profile by username",
            description = "Return public profile by username",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User profile not found")
            }
    )
    public ResponseEntity<ApiResponse<UserViewProfileResponse>> getOtherProfile(@PathVariable String username) {
        return ResponseEntity.ok(ApiResponse.<UserViewProfileResponse>builder().data(getUserProfileUseCase.getProfileByUsername(username)).build());
    }
}


