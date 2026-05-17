package com.socialpulse.app.chat.infrastructure.config;

import java.security.Principal;
import java.util.regex.Pattern;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket security configuration that enforces destination-level authorization
 * for STOMP SUBSCRIBE and SEND commands.
 *
 * <p>Since the {@link com.socialpulse.app.chat.infrastructure.websocket.WebSocketAuthInterceptor}
 * already validates JWT tokens on CONNECT and sets the authenticated user principal,
 * this configuration adds a second layer of security by ensuring:
 * <ul>
 *   <li>Only authenticated users can subscribe to {@code /topic/chat.*} and {@code /user/queue/*}</li>
 *   <li>Only authenticated users can send messages to {@code /app/chat.*}</li>
 * </ul>
 *
 * <p>This approach is used instead of {@code @EnableWebSocketSecurity} because
 * {@code spring-security-messaging} is not included as a project dependency.
 * The interceptor-based approach provides equivalent security guarantees at the
 * STOMP message level.
 */
@Configuration
@Order(Ordered.LOWEST_PRECEDENCE)
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

    private static final Pattern TOPIC_CHAT_PATTERN = Pattern.compile("^/topic/chat\\..+$");
    private static final Pattern USER_QUEUE_PATTERN = Pattern.compile("^/user/queue/.+$");
    private static final Pattern APP_CHAT_PATTERN = Pattern.compile("^/app/chat\\..+$");

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new WebSocketSecurityInterceptor());
    }

    /**
     * Channel interceptor that enforces authentication on secured STOMP destinations.
     * Runs after the auth interceptor has set the user principal on CONNECT.
     */
    static class WebSocketSecurityInterceptor implements ChannelInterceptor {

        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                    message, StompHeaderAccessor.class);

            if (accessor == null) {
                return message;
            }

            StompCommand command = accessor.getCommand();

            if (command == StompCommand.SUBSCRIBE) {
                enforceSubscribeAuthorization(accessor);
            } else if (command == StompCommand.SEND) {
                enforceSendAuthorization(accessor);
            }

            return message;
        }

        /**
         * Ensures only authenticated users can subscribe to secured destinations:
         * - /topic/chat.* (conversation message topics)
         * - /user/queue/* (user-specific queues like error notifications)
         */
        private void enforceSubscribeAuthorization(StompHeaderAccessor accessor) {
            String destination = accessor.getDestination();
            if (destination == null) {
                return;
            }

            boolean isSecuredDestination = TOPIC_CHAT_PATTERN.matcher(destination).matches()
                    || USER_QUEUE_PATTERN.matcher(destination).matches();

            if (isSecuredDestination) {
                requireAuthentication(accessor, "subscribe to " + destination);
            }
        }

        /**
         * Ensures only authenticated users can send to secured destinations:
         * - /app/chat.* (chat application endpoints)
         */
        private void enforceSendAuthorization(StompHeaderAccessor accessor) {
            String destination = accessor.getDestination();
            if (destination == null) {
                return;
            }

            if (APP_CHAT_PATTERN.matcher(destination).matches()) {
                requireAuthentication(accessor, "send to " + destination);
            }
        }

        /**
         * Verifies that the STOMP session has an authenticated user principal.
         * The principal is set by the WebSocketAuthInterceptor during CONNECT.
         *
         * @throws MessageDeliveryException if no authenticated user is found
         */
        private void requireAuthentication(StompHeaderAccessor accessor, String action) {
            Principal user = accessor.getUser();
            if (user == null) {
                throw new MessageDeliveryException(
                        "Access denied: Authentication required to " + action);
            }
        }
    }
}
