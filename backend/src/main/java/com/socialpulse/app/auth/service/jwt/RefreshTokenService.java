package com.socialpulse.app.auth.service.jwt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.auth.dto.TokenPair;
import com.socialpulse.app.auth.entity.RefreshToken;
import com.socialpulse.app.auth.repository.RefreshTokenRepository;
import com.socialpulse.app.auth.security.user.CustomUserDetails;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.status.AuthCode;
import com.socialpulse.app.user.entity.User;

@Service
public class RefreshTokenService {

    private static final int REFRESH_TOKEN_NUM_BYTES = 64;

    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom;

    public RefreshTokenService(JwtService jwtService, RefreshTokenRepository refreshTokenRepository) {
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.secureRandom = new SecureRandom();
    }

    @Transactional
    public String issueRefreshToken(User user) {
        String rawToken = generateToken();
        String tokenHash = hashToken(rawToken);

        LocalDateTime now = LocalDateTime.now();
        long refreshTtlMs = jwtService.getJwtProperties().getRefreshExpirationMs();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(now.plusNanos(refreshTtlMs * 1_000_000L))
                .build();

        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    @Transactional
    public TokenPair rotateTokens(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new AppException(AuthCode.INVALID_REFRESH_TOKEN);
        }

        String tokenHash = hashToken(rawRefreshToken);
        RefreshToken tokenRecord = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new AppException(AuthCode.INVALID_REFRESH_TOKEN));

        LocalDateTime now = LocalDateTime.now();

        if (tokenRecord.isRevoked()) {
            revokeAllActiveTokensForUser(tokenRecord.getUser().getId(), now);
            throw new AppException(AuthCode.REFRESH_TOKEN_REUSE_DETECTED);
        }

        if (tokenRecord.isExpired(now)) {
            throw new AppException(AuthCode.INVALID_REFRESH_TOKEN);
        }

        User user = tokenRecord.getUser();

        String newRawRefreshToken = generateToken();
        String newTokenHash = hashToken(newRawRefreshToken);

        long refreshTtlMs = jwtService.getJwtProperties().getRefreshExpirationMs();
        RefreshToken newTokenRecord = RefreshToken.builder()
                .user(user)
                .tokenHash(newTokenHash)
                .expiresAt(now.plusNanos(refreshTtlMs * 1_000_000L))
                .build();

        refreshTokenRepository.save(newTokenRecord);

        tokenRecord.setRevokedAt(now);
        tokenRecord.setReplacedByToken(newTokenRecord.getId());
        refreshTokenRepository.save(tokenRecord);

        CustomUserDetails userDetails = CustomUserDetails.builder()
                .user(user)
                .build();

        String newAccessToken = jwtService.generateToken(userDetails);
        return new TokenPair(newAccessToken, newRawRefreshToken);
    }

    @Transactional
    public void revokeCurrentToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }

        String tokenHash = hashToken(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(token -> {
                    if (!token.isRevoked()) {
                        token.setRevokedAt(LocalDateTime.now());
                        refreshTokenRepository.save(token);
                    }
                });
    }

    @Transactional
    public void revokeAllActiveTokensForUser(Long userId, LocalDateTime revokedAt) {
        List<RefreshToken> activeTokens = refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(userId);
        if (activeTokens.isEmpty()) {
            return;
        }

        for (RefreshToken token : activeTokens) {
            token.setRevokedAt(revokedAt);
        }

        refreshTokenRepository.saveAll(activeTokens);
    }

    private String generateToken() {
        byte[] randomBytes = new byte[REFRESH_TOKEN_NUM_BYTES];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }
}
