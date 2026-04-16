package com.socialpulse.app.auth.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.socialpulse.app.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "refresh_tokens", indexes = {
		@Index(name = "idx_refresh_tokens_user", columnList = "user_id"),
		@Index(name = "idx_refresh_tokens_hash", columnList = "token_hash", unique = true),
		@Index(name = "idx_refresh_tokens_expires", columnList = "expires_at")
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class RefreshToken {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "token_hash", nullable = false, length = 64, unique = true)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "revoked_at")
	private LocalDateTime revokedAt;

	@Column(name = "replaced_by_token")
	private UUID replacedByToken;

	public boolean isExpired(LocalDateTime now) {
		return expiresAt == null || expiresAt.isBefore(now);
	}

	public boolean isRevoked() {
		return revokedAt != null;
	}
}
