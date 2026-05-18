package com.socialpulse.app.auth.application.service;

import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.socialpulse.app.auth.application.dto.request.EmailVerificationRequest;
import com.socialpulse.app.auth.application.usecase.OtpUseCase;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.user.domain.enums.UserStatus;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class VerifyEmailServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private OtpUseCase otpUseCase;

    private VerifyEmailService service;

    @BeforeEach
    void setUp() {
        service = new VerifyEmailService(userRepository, otpUseCase);
    }

    @Test
    void verifyEmail_activatesAccount() {
        User user = User.builder().id(1L).email("u@mail.com").status(UserStatus.PENDING).build();
        when(userRepository.findByEmail("u@mail.com")).thenReturn(Optional.of(user));

        service.verifyEmail(new EmailVerificationRequest("u@mail.com", "123456"));

        verify(otpUseCase).verifyOtp("u@mail.com", "123456");
        verify(otpUseCase).invalidateOtp("u@mail.com");
        verify(userRepository).save(user);
    }

    @Test
    void verifyEmail_alreadyActive_skips() {
        User user = User.builder().id(1L).email("u@mail.com").status(UserStatus.ACTIVE).build();
        when(userRepository.findByEmail("u@mail.com")).thenReturn(Optional.of(user));

        service.verifyEmail(new EmailVerificationRequest("u@mail.com", "123456"));

        verify(otpUseCase).invalidateOtp("u@mail.com");
        verify(otpUseCase, never()).verifyOtp(any(), any());
    }

    @Test
    void verifyEmail_userNotFound_throws() {
        when(userRepository.findByEmail("x@mail.com")).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> service.verifyEmail(new EmailVerificationRequest("x@mail.com", "123")));
    }
}
