package com.socialpulse.app.chat.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.socialpulse.app.chat.application.dto.response.MessageHistoryResponse;
import com.socialpulse.app.chat.domain.exception.ConversationNotFoundException;
import com.socialpulse.app.chat.domain.exception.MessageValidationException;
import com.socialpulse.app.chat.domain.exception.UnauthorizedChatAccessException;
import com.socialpulse.app.chat.domain.model.Conversation;
import com.socialpulse.app.chat.domain.model.Message;
import com.socialpulse.app.chat.domain.model.MessageStatus;
import com.socialpulse.app.chat.domain.repository.ConversationRepository;
import com.socialpulse.app.chat.domain.repository.MessageRepository;
import com.socialpulse.app.security.user.CustomUserDetails;

@ExtendWith(MockitoExtension.class)
class GetMessageHistoryServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private CustomUserDetails currentUser;

    private GetMessageHistoryService service;

    private static final Long CONVERSATION_ID = 1L;
    private static final Long USER_ID = 100L;
    private static final Long OTHER_USER_ID = 200L;

    @BeforeEach
    void setUp() {
        service = new GetMessageHistoryService(conversationRepository, messageRepository);
    }

    @Test
    void getHistory_conversationNotFound_throwsConversationNotFoundException() {
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getHistory(CONVERSATION_ID, null, 20, currentUser))
                .isInstanceOf(ConversationNotFoundException.class);
    }

    @Test
    void getHistory_userNotParticipant_throwsUnauthorizedChatAccessException() {
        Conversation conversation = Conversation.builder()
                .id(CONVERSATION_ID)
                .participant1Id(300L)
                .participant2Id(400L)
                .build();
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
        when(currentUser.getId()).thenReturn(USER_ID);

        assertThatThrownBy(() -> service.getHistory(CONVERSATION_ID, null, 20, currentUser))
                .isInstanceOf(UnauthorizedChatAccessException.class);
    }

    @Test
    void getHistory_invalidCursorFormat_throwsMessageValidationException() {
        Conversation conversation = buildConversation();
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
        when(currentUser.getId()).thenReturn(USER_ID);

        assertThatThrownBy(() -> service.getHistory(CONVERSATION_ID, "not-a-timestamp", 20, currentUser))
                .isInstanceOf(MessageValidationException.class)
                .hasMessage("Invalid cursor format");
    }

    @Test
    void getHistory_nullCursor_usesCurrentTime() {
        Conversation conversation = buildConversation();
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
        when(currentUser.getId()).thenReturn(USER_ID);
        when(messageRepository.findByConversationIdBefore(eq(CONVERSATION_ID), any(Instant.class), eq(21)))
                .thenReturn(Collections.emptyList());

        MessageHistoryResponse response = service.getHistory(CONVERSATION_ID, null, 20, currentUser);

        assertThat(response.messages()).isEmpty();
        assertThat(response.hasMore()).isFalse();
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    void getHistory_emptyCursor_usesCurrentTime() {
        Conversation conversation = buildConversation();
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
        when(currentUser.getId()).thenReturn(USER_ID);
        when(messageRepository.findByConversationIdBefore(eq(CONVERSATION_ID), any(Instant.class), eq(21)))
                .thenReturn(Collections.emptyList());

        MessageHistoryResponse response = service.getHistory(CONVERSATION_ID, "", 20, currentUser);

        assertThat(response.messages()).isEmpty();
        assertThat(response.hasMore()).isFalse();
    }

    @Test
    void getHistory_validCursor_parsesCorrectly() {
        Conversation conversation = buildConversation();
        String cursor = "2024-01-15T10:30:00Z";
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
        when(currentUser.getId()).thenReturn(USER_ID);
        when(messageRepository.findByConversationIdBefore(eq(CONVERSATION_ID), eq(Instant.parse(cursor)), eq(21)))
                .thenReturn(Collections.emptyList());

        MessageHistoryResponse response = service.getHistory(CONVERSATION_ID, cursor, 20, currentUser);

        assertThat(response.messages()).isEmpty();
        assertThat(response.hasMore()).isFalse();
    }

    @Test
    void getHistory_messagesExistWithMore_returnsHasMoreTrueAndNextCursor() {
        Conversation conversation = buildConversation();
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
        when(currentUser.getId()).thenReturn(USER_ID);

        // Return 3 messages when requesting size 2 (size + 1 = 3)
        Instant t1 = Instant.parse("2024-01-15T10:03:00Z");
        Instant t2 = Instant.parse("2024-01-15T10:02:00Z");
        Instant t3 = Instant.parse("2024-01-15T10:01:00Z");
        List<Message> messages = List.of(
                buildMessage(1L, t1),
                buildMessage(2L, t2),
                buildMessage(3L, t3));

        when(messageRepository.findByConversationIdBefore(eq(CONVERSATION_ID), any(Instant.class), eq(3)))
                .thenReturn(messages);

        MessageHistoryResponse response = service.getHistory(CONVERSATION_ID, null, 2, currentUser);

        assertThat(response.messages()).hasSize(2);
        assertThat(response.hasMore()).isTrue();
        assertThat(response.nextCursor()).isEqualTo(t2.toString());
    }

    @Test
    void getHistory_messagesExistWithNoMore_returnsHasMoreFalseAndNullCursor() {
        Conversation conversation = buildConversation();
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
        when(currentUser.getId()).thenReturn(USER_ID);

        Instant t1 = Instant.parse("2024-01-15T10:02:00Z");
        Instant t2 = Instant.parse("2024-01-15T10:01:00Z");
        List<Message> messages = List.of(
                buildMessage(1L, t1),
                buildMessage(2L, t2));

        when(messageRepository.findByConversationIdBefore(eq(CONVERSATION_ID), any(Instant.class), eq(21)))
                .thenReturn(messages);

        MessageHistoryResponse response = service.getHistory(CONVERSATION_ID, null, 20, currentUser);

        assertThat(response.messages()).hasSize(2);
        assertThat(response.hasMore()).isFalse();
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    void getHistory_pageSizeZero_defaultsTo20() {
        Conversation conversation = buildConversation();
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
        when(currentUser.getId()).thenReturn(USER_ID);
        when(messageRepository.findByConversationIdBefore(eq(CONVERSATION_ID), any(Instant.class), eq(21)))
                .thenReturn(Collections.emptyList());

        service.getHistory(CONVERSATION_ID, null, 0, currentUser);

        // Verify it requested 21 (default 20 + 1)
        org.mockito.Mockito.verify(messageRepository)
                .findByConversationIdBefore(eq(CONVERSATION_ID), any(Instant.class), eq(21));
    }

    @Test
    void getHistory_pageSizeNegative_defaultsTo20() {
        Conversation conversation = buildConversation();
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
        when(currentUser.getId()).thenReturn(USER_ID);
        when(messageRepository.findByConversationIdBefore(eq(CONVERSATION_ID), any(Instant.class), eq(21)))
                .thenReturn(Collections.emptyList());

        service.getHistory(CONVERSATION_ID, null, -5, currentUser);

        org.mockito.Mockito.verify(messageRepository)
                .findByConversationIdBefore(eq(CONVERSATION_ID), any(Instant.class), eq(21));
    }

    @Test
    void getHistory_pageSizeExceeds50_clampedTo50() {
        Conversation conversation = buildConversation();
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
        when(currentUser.getId()).thenReturn(USER_ID);
        when(messageRepository.findByConversationIdBefore(eq(CONVERSATION_ID), any(Instant.class), eq(51)))
                .thenReturn(Collections.emptyList());

        service.getHistory(CONVERSATION_ID, null, 100, currentUser);

        // Verify it requested 51 (clamped 50 + 1)
        org.mockito.Mockito.verify(messageRepository)
                .findByConversationIdBefore(eq(CONVERSATION_ID), any(Instant.class), eq(51));
    }

    @Test
    void getHistory_messageResponseMappedCorrectly() {
        Conversation conversation = buildConversation();
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
        when(currentUser.getId()).thenReturn(USER_ID);

        Instant timestamp = Instant.parse("2024-01-15T10:00:00Z");
        Message message = Message.builder()
                .id(42L)
                .conversationId(CONVERSATION_ID)
                .senderId(USER_ID)
                .content("Hello world")
                .timestamp(timestamp)
                .status(MessageStatus.SENT)
                .build();

        when(messageRepository.findByConversationIdBefore(eq(CONVERSATION_ID), any(Instant.class), eq(21)))
                .thenReturn(List.of(message));

        MessageHistoryResponse response = service.getHistory(CONVERSATION_ID, null, 20, currentUser);

        assertThat(response.messages()).hasSize(1);
        var msgResponse = response.messages().get(0);
        assertThat(msgResponse.id()).isEqualTo(42L);
        assertThat(msgResponse.conversationId()).isEqualTo(CONVERSATION_ID);
        assertThat(msgResponse.senderId()).isEqualTo(USER_ID);
        assertThat(msgResponse.content()).isEqualTo("Hello world");
        assertThat(msgResponse.timestamp()).isEqualTo(timestamp);
        assertThat(msgResponse.status()).isEqualTo(MessageStatus.SENT);
    }

    private Conversation buildConversation() {
        return Conversation.builder()
                .id(CONVERSATION_ID)
                .participant1Id(USER_ID)
                .participant2Id(OTHER_USER_ID)
                .createdAt(Instant.now())
                .build();
    }

    private Message buildMessage(Long id, Instant timestamp) {
        return Message.builder()
                .id(id)
                .conversationId(CONVERSATION_ID)
                .senderId(OTHER_USER_ID)
                .content("Message " + id)
                .timestamp(timestamp)
                .status(MessageStatus.SENT)
                .build();
    }
}
