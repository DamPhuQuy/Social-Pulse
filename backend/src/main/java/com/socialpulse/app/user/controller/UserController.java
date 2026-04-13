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

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserViewProfileResponse> getProfile(@AuthenticationPrincipal CustomUserDetails currentUser) {
        Long userId = currentUser.getId();

        return ResponseEntity.ok(userService.getProfile(userId));
    }

    @GetMapping("/profile/{username}")
    @PreAuthorize("hasAuthority('VIEW_OTHER_PROFILE')")
    public ResponseEntity<UserViewProfileResponse> getOtherProfile(@AuthenticationPrincipal CustomUserDetails currentUser, @PathVariable String username) {
        return ResponseEntity.ok(userService.getProfileByUsername(username));
    }
}
