package com.socialpulse.app.user.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.security.encoder.AppPasswordEncoder;
import com.socialpulse.app.user.application.dto.request.ChangePasswordRequest;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ChangePasswordServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AppPasswordEncoder passwordEncoder;

    private ChangePasswordService service;

    @BeforeEach
    void setUp() {
        service = new ChangePasswordService(userRepository, passwordEncoder);
    }

    @Test
    void changePassword_success() {
        User user = User.builder().id(1L).passwordHash("old-hash").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPass", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("newPass", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("newPass")).thenReturn("new-hash");

        service.changePassword(1L, new ChangePasswordRequest("oldPass", "newPass", "newPass"));

        verify(userRepository).save(user);
    }

    @Test
    void changePassword_wrongCurrentPassword_throws() {
        User user = User.builder().id(1L).passwordHash("old-hash").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

        assertThrows(AppException.class, () ->
                service.changePassword(1L, new ChangePasswordRequest("wrong", "newPass", "newPass")));
    }

    @Test
    void changePassword_mismatchConfirm_throws() {
        User user = User.builder().id(1L).passwordHash("old-hash").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPass", "old-hash")).thenReturn(true);

        assertThrows(AppException.class, () ->
                service.changePassword(1L, new ChangePasswordRequest("oldPass", "newPass", "different")));
    }

    @Test
    void changePassword_sameAsOld_throws() {
        User user = User.builder().id(1L).passwordHash("old-hash").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPass", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("oldPass", "old-hash")).thenReturn(true);

        assertThrows(AppException.class, () ->
                service.changePassword(1L, new ChangePasswordRequest("oldPass", "oldPass", "oldPass")));
    }
}
