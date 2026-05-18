package com.socialpulse.app.chat.application.service;

import java.time.Instant;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.chat.application.dto.request.SendMessageRequest;
import com.socialpulse.app.chat.application.dto.response.MessageResponse;
import com.socialpulse.app.chat.application.usecase.SendMessageUseCase;
import com.socialpulse.app.chat.domain.event.MessagePersistedEvent;
import com.socialpulse.app.chat.domain.exception.ConversationNotFoundException;
import com.socialpulse.app.chat.domain.exception.MessagePersistenceException;
import com.socialpulse.app.chat.domain.exception.MessageValidationException;
import com.socialpulse.app.chat.domain.exception.UnauthorizedChatAccessException;
import com.socialpulse.app.chat.domain.model.Conversation;
import com.socialpulse.app.chat.domain.model.Message;
import com.socialpulse.app.chat.domain.model.MessageStatus;
import com.socialpulse.app.chat.domain.repository.ConversationRepository;
import com.socialpulse.app.chat.domain.repository.MessageRepository;
import com.socialpulse.app.security.user.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class SendMessageService implements SendMessageUseCase {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public MessageResponse sendMessage(Long conversationId, SendMessageRequest request,
                                       CustomUserDetails sender) {
        String content = request.content();

        // 1. Validate content: reject null, blank (whitespace-only), or length > 2000
        validateContent(content);

        // 2. Find conversation by ID: throw not-found if missing
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        // 3. Check if sender is a participant
        Long senderId = sender.getId();
        if (!conversation.hasParticipant(senderId)) {
            throw new UnauthorizedChatAccessException(senderId, conversationId);
        }

        // 4. Build Message domain object
        Instant now = Instant.now();
        Message message = Message.builder()
                .conversationId(conversationId)
                .senderId(senderId)
                .content(content)
                .timestamp(now)
                .status(MessageStatus.SENT)
                .build();

        // 5. Persist message — on failure, throw MessagePersistenceException
        Message savedMessage;
        try {
            savedMessage = messageRepository.save(message);
        } catch (MessagePersistenceException e) {
            throw e;
        } catch (Exception e) {
            throw new MessagePersistenceException("Failed to persist message", e);
        }

        // 6. Update conversation's lastMessageAt
        conversationRepository.updateLastMessageTimestamp(conversationId, savedMessage.getTimestamp());

        // 7. Publish domain event for message delivery
        Long recipientId = conversation.getOtherParticipant(senderId);
        applicationEventPublisher.publishEvent(new MessagePersistedEvent(savedMessage, recipientId));

        // 8. Return MessageResponse
        return MessageResponse.builder()
                .id(savedMessage.getId())
                .conversationId(savedMessage.getConversationId())
                .senderId(savedMessage.getSenderId())
                .content(savedMessage.getContent())
                .timestamp(savedMessage.getTimestamp())
                .status(savedMessage.getStatus())
                .build();
    }

    private void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new MessageValidationException(
                    "Message content must not be empty or whitespace-only");
        }
        if (content.length() > 2000) {
            throw new MessageValidationException(
                    "Message content must not exceed 2000 characters");
        }
    }
}
