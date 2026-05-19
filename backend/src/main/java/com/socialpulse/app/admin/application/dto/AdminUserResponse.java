package com.socialpulse.app.admin.application.dto;

import java.time.LocalDateTime;
import java.util.Set;

import com.socialpulse.app.user.domain.enums.UserStatus;
import com.socialpulse.app.user.domain.enums.VerificationStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminUserResponse {
    private Long id;
    private String username;
    private String email;
    private String displayName;
    private String avatarUrl;
    private UserStatus status;
    private VerificationStatus verification;
    private boolean isLocked;
    private int failedLoginAttempts;
    private Set<String> roles;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}
