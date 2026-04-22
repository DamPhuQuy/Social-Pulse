package com.socialpulse.app.auth.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class RefreshToken {
    private UUID id;
    private Long userId;
    private String tokenHash;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime revokedAt;
    private UUID replacedByToken;

    public boolean isExpired(LocalDateTime now) {
        return expiresAt == null || expiresAt.isBefore(now);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

}
