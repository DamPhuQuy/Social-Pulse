package com.socialpulse.app.notification.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.socialpulse.app.realtime.application.service.SseEmitterRegistry;
import com.socialpulse.app.notification.domain.model.Notification;
import com.socialpulse.app.notification.domain.repository.NotificationRepository;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class NotificationCommandServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private UserRepository userRepository;
    @Mock private SseEmitterRegistry sseEmitterRegistry;

    private NotificationCommandService service;

    @BeforeEach
    void setUp() {
        service = new NotificationCommandService(notificationRepository, userRepository, sseEmitterRegistry);
    }

    @Test
    void notifyFollow_savesAndSendsRealtimeSSE() {
        User actor = User.builder().id(1L).username("actor").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        Notification savedNotification = Notification.builder().id(1L).actorId(1L).recipientId(2L).build();
        when(notificationRepository.save(any())).thenReturn(savedNotification);

        service.notifyFollow(1L, 2L);

        verify(notificationRepository).save(any());
        verify(sseEmitterRegistry).sendToUser(eq(2L), eq("notification"), eq(savedNotification));
    }

    @Test
    void notifyFollow_selfNotification_skipped() {
        service.notifyFollow(1L, 1L);

        verifyNoInteractions(notificationRepository);
        verifyNoInteractions(sseEmitterRegistry);
    }

    @Test
    void notifyFollow_nullIds_skipped() {
        service.notifyFollow(null, 2L);
        service.notifyFollow(1L, null);

        verifyNoInteractions(notificationRepository);
        verifyNoInteractions(sseEmitterRegistry);
    }
}
