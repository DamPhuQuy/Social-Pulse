package com.socialpulse.app.chat.adapter.web;

import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import com.socialpulse.app.chat.application.dto.request.ReadAcknowledgmentRequest;
import com.socialpulse.app.chat.application.dto.request.SendMessageRequest;
import com.socialpulse.app.chat.application.usecase.AcknowledgeReadUseCase;
import com.socialpulse.app.chat.application.usecase.SendMessageUseCase;
import com.socialpulse.app.chat.domain.exception.ChatException;
import com.socialpulse.app.security.user.CustomUserDetails;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket STOMP controller for real-time chat operations.
 *
 * <p>Handles message sending and read acknowledgments via STOMP message mappings.
 * Authentication is extracted from the STOMP session principal set by
 * {@link com.socialpulse.app.chat.infrastructure.websocket.WebSocketAuthInterceptor}.
 *
 * <p>Message delivery to recipients is handled by
 * {@link com.socialpulse.app.chat.application.service.NotifyMessageService}
 * via domain events, so this controller only delegates to use cases.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final SendMessageUseCase sendMessageUseCase;
    private final AcknowledgeReadUseCase acknowledgeReadUseCase;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handles sending a chat message.
     *
     * <p>Extracts the authenticated user from the STOMP session and delegates
     * to {@link SendMessageUseCase}. The NotifyMessageService handles delivery
     * to the recipient via domain events.
     *
     * @param request the message payload containing conversationId and content
     * @param headerAccessor STOMP message headers containing the user principal
     */
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload SendMessageRequest request,
                            SimpMessageHeaderAccessor headerAccessor) {
        CustomUserDetails userDetails = extractUserDetails(headerAccessor);
        log.debug("User {} sending message to conversation {}",
                userDetails.getId(), request.conversationId());

        sendMessageUseCase.sendMessage(request.conversationId(), request, userDetails);
    }

    /**
     * Handles read acknowledgment for a conversation.
     *
     * <p>Marks all unread messages from the other participant as READ and
     * notifies the sender of the status change.
     *
     * @param request the acknowledgment payload containing conversationId
     * @param headerAccessor STOMP message headers containing the user principal
     */
    @MessageMapping("/chat.read")
    public void acknowledgeRead(@Payload ReadAcknowledgmentRequest request,
                                SimpMessageHeaderAccessor headerAccessor) {
        CustomUserDetails userDetails = extractUserDetails(headerAccessor);
        log.debug("User {} acknowledging read for conversation {}",
                userDetails.getId(), request.conversationId());

        acknowledgeReadUseCase.acknowledgeRead(request.conversationId(), userDetails);
    }

    /**
     * Handles ChatException subclasses thrown during WebSocket message processing.
     * Sends an error payload to the user's personal error queue.
     *
     * @param ex the chat exception that was thrown
     * @param headerAccessor STOMP message headers for identifying the user
     */
    @MessageExceptionHandler(ChatException.class)
    public void handleChatException(ChatException ex, SimpMessageHeaderAccessor headerAccessor) {
        String username = headerAccessor.getUser() != null
                ? headerAccessor.getUser().getName()
                : null;

        if (username == null) {
            log.warn("Cannot send error to user: no principal in STOMP session. Error: {}", ex.getMessage());
            return;
        }

        log.debug("Sending chat error to user {}: [{}] {}",
                username, ex.getErrorCode(), ex.getMessage());

        ChatErrorPayload errorPayload = ChatErrorPayload.of(ex.getErrorCode(), ex.getMessage());
        messagingTemplate.convertAndSendToUser(username, "/queue/errors", errorPayload);
    }

    /**
     * Extracts {@link CustomUserDetails} from the STOMP session's user principal.
     *
     * <p>The principal is set by the WebSocketAuthInterceptor during the CONNECT frame
     * as a {@link UsernamePasswordAuthenticationToken} with CustomUserDetails as the principal.
     *
     * @param headerAccessor the STOMP message header accessor
     * @return the authenticated user details
     * @throws IllegalStateException if no authenticated user is found in the session
     */
    private CustomUserDetails extractUserDetails(SimpMessageHeaderAccessor headerAccessor) {
        if (headerAccessor.getUser() instanceof UsernamePasswordAuthenticationToken authToken) {
            Object principal = authToken.getPrincipal();
            if (principal instanceof CustomUserDetails userDetails) {
                return userDetails;
            }
        }
        throw new IllegalStateException("No authenticated user found in STOMP session");
    }
}
