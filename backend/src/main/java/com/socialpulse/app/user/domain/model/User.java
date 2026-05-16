package com.socialpulse.app.user.domain.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.socialpulse.app.user.domain.enums.UserStatus;
import com.socialpulse.app.user.domain.enums.VerificationStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
	private Long id;
	private String username;
	private UserProfile profile;
	private String email;
	private String passwordHash;
	private UserStatus status;
	@Builder.Default
	private Set<Role> roles = new HashSet<>();
	private VerificationStatus verification;
	private boolean isLocked;
	private int failedLoginAttempts;
	private LocalDateTime lastLoginAt;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public void applyDefaultState() {
		if (this.roles == null || this.roles.isEmpty()) {
			this.roles = new HashSet<>();
		}

		if (this.verification == null) {
			this.verification = VerificationStatus.NOT_VERIFIED;
		}

		if (this.status == null) {
			this.status = UserStatus.PENDING;
		}
	}

	public void incrementFailedLoginAttempts() {
		this.failedLoginAttempts++;
	}

	public void resetFailedLoginAttempts() {
		this.failedLoginAttempts = 0;
	}

	public void updateLastLoginAt() {
		this.lastLoginAt = LocalDateTime.now();
	}

	public void pendingAccount() {
		this.status = UserStatus.PENDING;
	}

	public void activeAccount() {
		this.status = UserStatus.ACTIVE;
		this.isLocked = false;
	}

	public void verifyAccount() {
		this.verification = VerificationStatus.VERIFIED;
	}

	public void lockAccount() {
		this.status = UserStatus.LOCKED;
		this.isLocked = true;
	}

	public void changePassword(String newPasswordHash) {
		if (newPasswordHash == null || newPasswordHash.isBlank()) {
			throw new IllegalArgumentException("newPasswordHash must not be blank");
		}

		this.passwordHash = newPasswordHash;
	}
}
