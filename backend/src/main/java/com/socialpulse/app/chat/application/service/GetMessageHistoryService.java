package com.socialpulse.app.chat.application.service;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.chat.application.dto.response.MessageHistoryResponse;
import com.socialpulse.app.chat.application.dto.response.MessageResponse;
import com.socialpulse.app.chat.application.usecase.GetMessageHistoryUseCase;
import com.socialpulse.app.chat.domain.exception.ConversationNotFoundException;
import com.socialpulse.app.chat.domain.exception.MessageValidationException;
import com.socialpulse.app.chat.domain.exception.UnauthorizedChatAccessException;
import com.socialpulse.app.chat.domain.model.Conversation;
import com.socialpulse.app.chat.domain.model.Message;
import com.socialpulse.app.chat.domain.repository.ConversationRepository;
import com.socialpulse.app.chat.domain.repository.MessageRepository;
import com.socialpulse.app.security.user.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetMessageHistoryService implements GetMessageHistoryUseCase {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 50;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    @Override
    public MessageHistoryResponse getHistory(Long conversationId, String cursor,
                                             int size, CustomUserDetails currentUser) {
        // 1. Find conversation by ID: throw not-found if missing
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        // 2. Verify user is a participant: throw forbidden if not
        Long userId = currentUser.getId();
        if (!conversation.hasParticipant(userId)) {
            throw new UnauthorizedChatAccessException(userId, conversationId);
        }

        // 3. Validate and clamp page size: min 1, max 50, default 20 if <= 0
        int pageSize = clampPageSize(size);

        // 4. Parse cursor
        Instant cursorInstant = parseCursor(cursor);

        // 5. Query messages: request size + 1 to determine if there are more
        List<Message> results = messageRepository.findByConversationIdBefore(
                conversationId, cursorInstant, pageSize + 1);

        // 6. Build response
        boolean hasMore = results.size() > pageSize;
        List<Message> messages = hasMore ? results.subList(0, pageSize) : results;

        String nextCursor = null;
        if (!messages.isEmpty()) {
            // nextCursor is the oldest message's timestamp (last in the descending list)
            Instant oldestTimestamp = messages.get(messages.size() - 1).getTimestamp();
            nextCursor = oldestTimestamp.toString();
        }

        // 7. Map messages to MessageResponse list
        List<MessageResponse> messageResponses = messages.stream()
                .map(this::toMessageResponse)
                .toList();

        // 8. Return MessageHistoryResponse
        return MessageHistoryResponse.builder()
                .messages(messageResponses)
                .nextCursor(hasMore ? nextCursor : null)
                .hasMore(hasMore)
                .build();
    }

    private int clampPageSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.clamp(size, MIN_PAGE_SIZE, MAX_PAGE_SIZE);
    }

    private Instant parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.parse(cursor);
        } catch (DateTimeParseException e) {
            throw new MessageValidationException("Invalid cursor format");
        }
    }

    private MessageResponse toMessageResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .timestamp(message.getTimestamp())
                .status(message.getStatus())
                .build();
    }
}
