package com.socialpulse.app.auth.application.port.in;

import java.time.LocalDateTime;

public interface RefreshTokenRevocationUseCase {
    void revokeCurrentToken(String rawRefreshToken);

    void revokeAllActiveTokensForUser(Long userId, LocalDateTime revokedAt);
}
