package com.socialpulse.app.auth.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import com.socialpulse.app.auth.application.dto.TokenPair;
import com.socialpulse.app.auth.application.dto.mapper.AuthMapper;
import com.socialpulse.app.auth.application.dto.request.LoginRequest;
import com.socialpulse.app.auth.application.usecase.JwtUseCase;
import com.socialpulse.app.auth.application.usecase.RefreshTokenUseCase;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.security.user.CustomUserDetails;
import com.socialpulse.app.user.domain.enums.UserStatus;
import com.socialpulse.app.user.domain.enums.VerificationStatus;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUseCase jwtUseCase;
    @Mock private RefreshTokenUseCase refreshTokenUseCase;
    @Mock private AuthMapper authMapper;

    private AuthenticationService service;

    @BeforeEach
    void setUp() {
        service = new AuthenticationService(userRepository, authenticationManager, jwtUseCase, refreshTokenUseCase, authMapper);
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest req = new LoginRequest();
        ReflectionTestUtils.setField(req, "email", email);
        ReflectionTestUtils.setField(req, "password", password);
        return req;
    }

    @Test
    void authenticate_success() {
        User user = User.builder().id(1L).email("test@mail.com").status(UserStatus.ACTIVE)
                .verification(VerificationStatus.VERIFIED).failedLoginAttempts(0).build();
        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));

        Authentication auth = mock(Authentication.class);
        CustomUserDetails details = mock(CustomUserDetails.class);
        when(auth.getPrincipal()).thenReturn(details);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtUseCase.generateToken(details)).thenReturn("access-token");
        when(refreshTokenUseCase.issueRefreshToken(user)).thenReturn("refresh-token");
        when(authMapper.toTokenPair("access-token", "refresh-token")).thenReturn(new TokenPair("access-token", "refresh-token"));

        TokenPair result = service.authenticate(loginRequest("test@mail.com", "password"));

        assertNotNull(result);
        verify(userRepository).save(any());
    }

    @Test
    void authenticate_userNotFound_throws() {
        when(userRepository.findByEmail("x@mail.com")).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> service.authenticate(loginRequest("x@mail.com", "pass")));
    }

    @Test
    void authenticate_lockedAccount_throws() {
        User user = User.builder().id(1L).email("test@mail.com").status(UserStatus.ACTIVE)
                .verification(VerificationStatus.VERIFIED).isLocked(true).build();
        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));

        assertThrows(AppException.class, () -> service.authenticate(loginRequest("test@mail.com", "pass")));
    }

    @Test
    void authenticate_unverified_throws() {
        User user = User.builder().id(1L).email("test@mail.com").status(UserStatus.PENDING)
                .verification(VerificationStatus.NOT_VERIFIED).isLocked(false).build();
        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));

        assertThrows(AppException.class, () -> service.authenticate(loginRequest("test@mail.com", "pass")));
    }

    @Test
    void authenticate_badCredentials_incrementsFailedAttempts() {
        User user = User.builder().id(1L).email("test@mail.com").status(UserStatus.ACTIVE)
                .verification(VerificationStatus.VERIFIED).failedLoginAttempts(0).build();
        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThrows(AppException.class, () -> service.authenticate(loginRequest("test@mail.com", "wrong")));
        verify(userRepository).save(user);
    }
}
