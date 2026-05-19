package com.socialpulse.app.notification.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.socialpulse.app.common.websocket.WebSocketSessionManager;
import com.socialpulse.app.notification.application.dto.response.NotificationResponse;
import com.socialpulse.app.notification.domain.model.Notification;
import com.socialpulse.app.notification.domain.repository.NotificationRepository;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class NotificationCommandServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private UserRepository userRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private WebSocketSessionManager sessionManager;
    @Mock private NotificationResponseAssembler responseAssembler;

    private NotificationCommandService service;

    @BeforeEach
    void setUp() {
        service = new NotificationCommandService(notificationRepository, userRepository,
                messagingTemplate, sessionManager, responseAssembler);
    }

    @Test
    void notifyFollow_savesAndPushesWhenOnline() {
        User actor = User.builder().id(1L).username("actor").build();
        User recipient = User.builder().id(2L).username("recipient").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(notificationRepository.save(any())).thenReturn(Notification.builder().id(1L).actorId(1L).recipientId(2L).build());
        when(sessionManager.isUserOnline(2L)).thenReturn(true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));
        when(responseAssembler.assemble(any())).thenReturn(List.of(NotificationResponse.builder().id(1L).build()));

        service.notifyFollow(1L, 2L);

        verify(notificationRepository).save(any());
        verify(messagingTemplate).convertAndSendToUser(eq("recipient"), eq("/queue/notifications"), any());
    }

    @Test
    void notifyFollow_savesButNoPushWhenOffline() {
        User actor = User.builder().id(1L).username("actor").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(notificationRepository.save(any())).thenReturn(Notification.builder().id(1L).actorId(1L).recipientId(2L).build());
        when(sessionManager.isUserOnline(2L)).thenReturn(false);

        service.notifyFollow(1L, 2L);

        verify(notificationRepository).save(any());
        verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any(Object.class));
    }

    @Test
    void notifyFollow_selfNotification_skipped() {
        service.notifyFollow(1L, 1L);

        verifyNoInteractions(notificationRepository);
    }

    @Test
    void notifyFollow_nullIds_skipped() {
        service.notifyFollow(null, 2L);
        service.notifyFollow(1L, null);

        verifyNoInteractions(notificationRepository);
    }
}
