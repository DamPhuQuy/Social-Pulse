package com.socialpulse.app.auth.application.usecase;

import org.springframework.security.core.userdetails.UserDetails;

import com.socialpulse.app.security.user.CustomUserDetails;

public interface JwtUseCase {
    String generateToken(CustomUserDetails userDetails);

    String generateRefreshToken(CustomUserDetails userDetails);

    String extractEmail(String token);

    boolean isTokenValid(String token, UserDetails userDetails);

    long getAccessExpirationMs();

    long getRefreshExpirationMs();
}

