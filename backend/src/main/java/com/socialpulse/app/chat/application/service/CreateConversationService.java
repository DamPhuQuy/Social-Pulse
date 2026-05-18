package com.socialpulse.app.chat.application.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.socialpulse.app.chat.application.dto.request.CreateConversationRequest;
import com.socialpulse.app.chat.application.dto.response.ConversationResponse;
import com.socialpulse.app.chat.application.usecase.CreateConversationUseCase;
import com.socialpulse.app.chat.domain.exception.MessageValidationException;
import com.socialpulse.app.chat.domain.model.Conversation;
import com.socialpulse.app.chat.domain.repository.ConversationRepository;
import com.socialpulse.app.security.user.CustomUserDetails;
import com.socialpulse.app.user.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateConversationService implements CreateConversationUseCase {

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;

    @Override
    public ConversationResponse createConversation(CreateConversationRequest request,
                                                   CustomUserDetails currentUser) {
        Long currentUserId = currentUser.getId();
        Long participantId = request.participantId();

        // Validate: reject self-conversation
        if (currentUserId.equals(participantId)) {
            throw new MessageValidationException("Cannot create conversation with yourself");
        }

        // Validate: reject non-existent user
        if (userRepository.findById(participantId).isEmpty()) {
            throw new MessageValidationException("User not found");
        }

        // Normalize participant order (smaller ID as participant1) to enforce uniqueness constraint
        Long participant1Id = Math.min(currentUserId, participantId);
        Long participant2Id = Math.max(currentUserId, participantId);

        // Check for existing conversation between the pair; return existing if found
        return conversationRepository.findByParticipants(participant1Id, participant2Id)
                .map(this::toResponse)
                .orElseGet(() -> {
                    Conversation conversation = Conversation.builder()
                            .participant1Id(participant1Id)
                            .participant2Id(participant2Id)
                            .createdAt(Instant.now())
                            .build();
                    Conversation saved = conversationRepository.save(conversation);
                    return toResponse(saved);
                });
    }

    private ConversationResponse toResponse(Conversation conversation) {
        return ConversationResponse.builder()
                .id(conversation.getId())
                .participant1Id(conversation.getParticipant1Id())
                .participant2Id(conversation.getParticipant2Id())
                .createdAt(conversation.getCreatedAt())
                .lastMessageAt(conversation.getLastMessageAt())
                .build();
    }
}
