package com.socialpulse.app.user.application.dto.response;

import java.time.LocalDateTime;
import java.util.Set;

import com.socialpulse.app.user.domain.enums.UserStatus;
import com.socialpulse.app.user.domain.enums.VerificationStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AdminUserResponse {
    private Long id;
    private String username;
    private String email;
    private UserStatus status;
    private VerificationStatus verification;
    private boolean isLocked;
    private int failedLoginAttempts;
    private Set<String> roles;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Profile information
    private String displayName;
    private String avatarUrl;
    private String bio;
}
