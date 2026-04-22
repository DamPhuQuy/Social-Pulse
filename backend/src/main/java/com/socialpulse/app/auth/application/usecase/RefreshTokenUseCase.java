package com.socialpulse.app.auth.application.usecase;

import com.socialpulse.app.auth.application.dto.TokenPair;
import com.socialpulse.app.user.domain.model.User;

public interface RefreshTokenUseCase {
    String issueRefreshToken(User user);

    TokenPair rotateTokens(String rawRefreshToken);

    void revokeCurrentToken(String rawRefreshToken);
}

