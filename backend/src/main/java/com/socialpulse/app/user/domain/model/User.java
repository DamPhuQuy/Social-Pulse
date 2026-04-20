package com.socialpulse.app.user.domain.model;

import java.time.LocalDateTime;
import java.util.List;

import com.socialpulse.app.auth.domain.model.RefreshToken;
import com.socialpulse.app.comment.domain.model.Comment;
import com.socialpulse.app.comment.domain.model.CommentReaction;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.user.domain.enums.UserRole;
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
	private List<Post> posts;
	private List<Comment> comments;
	private List<CommentReaction> commentReactions;
	private List<RefreshToken> refreshTokens;
	private String email;
	private String passwordHash;
	private UserStatus status;
	private UserRole role;
	private VerificationStatus verification;
	private boolean isLocked;
	private int failedLoginAttempts;
	private LocalDateTime lastLoginAt;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public void applyDefaultState() {
		if (this.role == null) {
			this.role = UserRole.USER;
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
