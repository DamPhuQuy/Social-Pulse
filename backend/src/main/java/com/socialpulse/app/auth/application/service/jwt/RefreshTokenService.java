package com.socialpulse.app.auth.application.service.jwt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.auth.application.dto.TokenPair;
import com.socialpulse.app.auth.application.dto.mapper.AuthMapper;
import com.socialpulse.app.auth.application.port.in.JwtUseCase;
import com.socialpulse.app.auth.application.port.in.RefreshTokenRevocationUseCase;
import com.socialpulse.app.auth.application.port.in.RefreshTokenUseCase;
import com.socialpulse.app.auth.application.port.out.RefreshTokenRepositoryPort;
import com.socialpulse.app.auth.domain.model.RefreshToken;
import com.socialpulse.app.auth.security.user.CustomUserDetails;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.AuthCode;
import com.socialpulse.app.user.application.port.out.UserRepositoryPort;
import com.socialpulse.app.user.domain.model.User;

@Service
public class RefreshTokenService implements RefreshTokenUseCase {

    private static final int REFRESH_TOKEN_NUM_BYTES = 64;

    private final JwtUseCase jwtUseCase;
    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final RefreshTokenRevocationUseCase refreshTokenRevocationUseCase;
    private final UserRepositoryPort userRepository;
    private final AuthMapper authMapper;
    private final SecureRandom secureRandom;

    public RefreshTokenService(JwtUseCase jwtUseCase,
                               RefreshTokenRepositoryPort refreshTokenRepository,
                               RefreshTokenRevocationUseCase refreshTokenRevocationUseCase,
                               UserRepositoryPort userRepository,
                               AuthMapper authMapper) {
        this.jwtUseCase = jwtUseCase;
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenRevocationUseCase = refreshTokenRevocationUseCase;
        this.userRepository = userRepository;
        this.authMapper = authMapper;
        this.secureRandom = new SecureRandom();
    }

    @Override
    @Transactional
    public String issueRefreshToken(User user) {
        String rawToken = generateToken();
        String tokenHash = hashToken(rawToken);

        LocalDateTime now = LocalDateTime.now();
        long refreshTtlMs = jwtUseCase.getRefreshExpirationMs();

        RefreshToken refreshToken = RefreshToken.builder()
            .userId(user.getId())
                .tokenHash(tokenHash)
                .expiresAt(now.plusNanos(refreshTtlMs * 1_000_000L))
                .build();

        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    @Override
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
            refreshTokenRevocationUseCase.revokeAllActiveTokensForUser(tokenRecord.getUserId(), now);
            throw new AppException(AuthCode.REFRESH_TOKEN_REUSE_DETECTED);
        }

        if (tokenRecord.isExpired(now)) {
            throw new AppException(AuthCode.INVALID_REFRESH_TOKEN);
        }

        User user = userRepository.findById(tokenRecord.getUserId())
            .orElseThrow(() -> new AppException(AuthCode.INVALID_REFRESH_TOKEN));

        String newRawRefreshToken = generateToken();
        String newTokenHash = hashToken(newRawRefreshToken);

        long refreshTtlMs = jwtUseCase.getRefreshExpirationMs();
        RefreshToken newTokenRecord = RefreshToken.builder()
            .userId(tokenRecord.getUserId())
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

        String newAccessToken = jwtUseCase.generateToken(userDetails);
        return authMapper.toTokenPair(newAccessToken, newRawRefreshToken);
    }

    @Override
    @Transactional
    public void revokeCurrentToken(String rawRefreshToken) {
        refreshTokenRevocationUseCase.revokeCurrentToken(rawRefreshToken);
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
