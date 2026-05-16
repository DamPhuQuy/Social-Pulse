package com.socialpulse.app.user.adapter.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.socialpulse.app.common.dto.response.ApiResponse;
import com.socialpulse.app.security.user.CustomUserDetails;
import com.socialpulse.app.user.application.dto.request.UserProfileMutationRequest;
import com.socialpulse.app.user.application.dto.request.UserViewProfileRequest;
import com.socialpulse.app.user.application.dto.response.UserViewProfileResponse;
import com.socialpulse.app.user.application.usecase.CreateUserProfileUseCase;
import com.socialpulse.app.user.application.usecase.DeleteUserProfileUseCase;
import com.socialpulse.app.user.application.usecase.GetUserProfileUseCase;
import com.socialpulse.app.user.application.usecase.UpdateUserProfileUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User profile APIs")
public class UserController {

    private final GetUserProfileUseCase getUserProfileUseCase;
    private final CreateUserProfileUseCase createUserProfileUseCase;
    private final UpdateUserProfileUseCase updateUserProfileUseCase;
    private final DeleteUserProfileUseCase deleteUserProfileUseCase;

    public UserController(GetUserProfileUseCase getUserProfileUseCase,
                          CreateUserProfileUseCase createUserProfileUseCase,
                          UpdateUserProfileUseCase updateUserProfileUseCase,
                          DeleteUserProfileUseCase deleteUserProfileUseCase) {
        this.getUserProfileUseCase = getUserProfileUseCase;
        this.createUserProfileUseCase = createUserProfileUseCase;
        this.updateUserProfileUseCase = updateUserProfileUseCase;
        this.deleteUserProfileUseCase = deleteUserProfileUseCase;
    }

    @GetMapping("/profile")
    @PreAuthorize("hasAuthority('user:read')")
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

    @PostMapping("/profile")
    @PreAuthorize("hasAuthority('user:create')")
    @Operation(summary = "Create my profile", description = "Create a profile for the current authenticated user")
    public ResponseEntity<ApiResponse<UserViewProfileResponse>> createProfile(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestBody @Valid UserProfileMutationRequest request) {
        return ResponseEntity.ok(ApiResponse.<UserViewProfileResponse>builder()
                .data(createUserProfileUseCase.createProfile(currentUser.getId(), request))
                .message("Profile created successfully")
                .build());
    }

    @PutMapping("/profile")
    @PreAuthorize("hasAuthority('user:update')")
    @Operation(summary = "Update my profile", description = "Update the profile of the current authenticated user")
    public ResponseEntity<ApiResponse<UserViewProfileResponse>> updateProfile(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestBody @Valid UserProfileMutationRequest request) {
        return ResponseEntity.ok(ApiResponse.<UserViewProfileResponse>builder()
                .data(updateUserProfileUseCase.updateProfile(currentUser.getId(), request))
                .message("Profile updated successfully")
                .build());
    }

    @DeleteMapping("/profile")
    @PreAuthorize("hasAuthority('user:delete')")
    @Operation(summary = "Delete my profile", description = "Delete the profile of the current authenticated user")
    public ResponseEntity<ApiResponse<Void>> deleteProfile(@AuthenticationPrincipal CustomUserDetails currentUser) {
        deleteUserProfileUseCase.deleteProfile(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .message("Profile deleted successfully")
                .build());
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

