package com.socialpulse.app.chat.application.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.socialpulse.app.chat.application.dto.response.ConversationListResponse;
import com.socialpulse.app.chat.application.usecase.GetConversationsUseCase;
import com.socialpulse.app.chat.domain.model.Conversation;
import com.socialpulse.app.chat.domain.model.Message;
import com.socialpulse.app.chat.domain.repository.ConversationRepository;
import com.socialpulse.app.chat.domain.repository.MessageRepository;
import com.socialpulse.app.security.user.CustomUserDetails;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetConversationsService implements GetConversationsUseCase {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PREVIEW_LENGTH = 100;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    @Override
    public List<ConversationListResponse> getConversations(int page, int size, CustomUserDetails currentUser) {
        int effectiveSize = size <= 0 ? DEFAULT_PAGE_SIZE : size;
        Long currentUserId = currentUser.getId();

        List<Conversation> conversations = conversationRepository.findByUserId(currentUserId, page, effectiveSize);

        if (conversations.isEmpty()) {
            return List.of();
        }

        // Batch-fetch other participant usernames
        Set<Long> otherParticipantIds = conversations.stream()
                .map(conv -> conv.getOtherParticipant(currentUserId))
                .collect(Collectors.toSet());

        Map<Long, String> usernameMap = userRepository.findByIds(otherParticipantIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));

        return conversations.stream()
                .map(conversation -> buildConversationListResponse(conversation, currentUserId, usernameMap))
                .toList();
    }

    private ConversationListResponse buildConversationListResponse(Conversation conversation,
                                                                    Long currentUserId,
                                                                    Map<Long, String> usernameMap) {
        Long otherParticipantId = conversation.getOtherParticipant(currentUserId);
        String otherParticipantUsername = usernameMap.getOrDefault(otherParticipantId, "Unknown");

        String lastMessagePreview = getLastMessagePreview(conversation.getId());
        long unreadCount = messageRepository.countUnread(conversation.getId(), currentUserId);

        return ConversationListResponse.builder()
                .id(conversation.getId())
                .otherParticipantId(otherParticipantId)
                .otherParticipantUsername(otherParticipantUsername)
                .lastMessagePreview(lastMessagePreview)
                .unreadCount((int) unreadCount)
                .lastMessageAt(conversation.getLastMessageAt())
                .build();
    }

    private String getLastMessagePreview(Long conversationId) {
        List<Message> messages = messageRepository.findByConversationIdBefore(
                conversationId, Instant.now(), 1);

        if (messages.isEmpty()) {
            return null;
        }

        String content = messages.get(0).getContent();
        if (content != null && content.length() > MAX_PREVIEW_LENGTH) {
            return content.substring(0, MAX_PREVIEW_LENGTH);
        }
        return content;
    }
}
