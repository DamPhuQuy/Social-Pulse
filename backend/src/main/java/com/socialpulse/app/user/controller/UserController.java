package com.socialpulse.app.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.socialpulse.app.auth.security.user.CustomUserDetails;
import com.socialpulse.app.user.dto.response.UserViewProfileResponse;
import com.socialpulse.app.user.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User profile APIs")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
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
    public ResponseEntity<UserViewProfileResponse> getProfile(@AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(userService.getProfile(currentUser));
    }

    @GetMapping("/profile/{username}")
    @PreAuthorize("hasAuthority('VIEW_OTHER_PROFILE')")
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
    public ResponseEntity<UserViewProfileResponse> getOtherProfile(@AuthenticationPrincipal CustomUserDetails currentUser, @PathVariable String username) {
        return ResponseEntity.ok(userService.getProfileByUsername(username.toLowerCase()));
    }
}
