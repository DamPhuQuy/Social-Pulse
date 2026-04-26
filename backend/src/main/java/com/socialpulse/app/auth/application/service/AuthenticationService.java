package com.socialpulse.app.auth.application.service;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.auth.application.dto.TokenPair;
import com.socialpulse.app.auth.application.dto.mapper.AuthMapper;
import com.socialpulse.app.auth.application.dto.request.LoginRequest;
import com.socialpulse.app.auth.application.usecase.JwtUseCase;
import com.socialpulse.app.auth.application.usecase.AuthenticationUseCase;
import com.socialpulse.app.auth.application.usecase.RefreshTokenUseCase;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.AuthCode;
import com.socialpulse.app.security.user.CustomUserDetails;
import com.socialpulse.app.user.domain.repository.UserRepository;
import com.socialpulse.app.user.domain.enums.UserStatus;
import com.socialpulse.app.user.domain.enums.VerificationStatus;
import com.socialpulse.app.user.domain.model.User;

public class AuthenticationService implements AuthenticationUseCase {

    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtUseCase jwtUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final AuthMapper authMapper;
    private final Logger logger;

    public AuthenticationService(UserRepository userRepository,
                                 AuthenticationManager authenticationManager,
                                 JwtUseCase jwtUseCase,
                                 RefreshTokenUseCase refreshTokenUseCase,
                                 AuthMapper authMapper) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.jwtUseCase = jwtUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.authMapper = authMapper;
        this.logger = LoggerFactory.getLogger(AuthenticationService.class);
    }

    @Override
    @Transactional(noRollbackFor = AppException.class)
    public TokenPair authenticate(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);

        var user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AppException(AuthCode.INVALID_CREDENTIALS));

        if (user.isLocked() || user.getStatus() == UserStatus.LOCKED) {
            throw new AppException(AuthCode.ACCOUNT_LOCKED);
        }

        if (user.getVerification() != VerificationStatus.VERIFIED || user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(AuthCode.ACCOUNT_NOT_VERIFIED);
        }

        try {
            org.springframework.security.core.Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword()));

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            user.resetFailedLoginAttempts();
            user.updateLastLoginAt();
            userRepository.save(user);

            String accessToken = jwtUseCase.generateToken(userDetails);
            String refreshToken = refreshTokenUseCase.issueRefreshToken(user);
            logger.info("Login successful for: {}", normalizedEmail);

            return authMapper.toTokenPair(accessToken, refreshToken);
        } catch (BadCredentialsException e) {
            handleFailedLoginAttempt(user);
            throw new AppException(AuthCode.INVALID_CREDENTIALS);
        } catch (LockedException e) {
            throw new AppException(AuthCode.ACCOUNT_LOCKED);
        } catch (DisabledException e) {
            throw new AppException(AuthCode.ACCOUNT_NOT_VERIFIED);
        }
    }

    private void handleFailedLoginAttempt(User user) {
        user.incrementFailedLoginAttempts();
        if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
            user.lockAccount();
            logger.warn("Account locked after {} failed attempts: {}", MAX_FAILED_ATTEMPTS, user.getEmail());
        }
        userRepository.save(user);
    }
}


