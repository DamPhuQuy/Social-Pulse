package com.socialpulse.app.common.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;

import com.socialpulse.app.auth.application.usecase.JwtUseCase;
import com.socialpulse.app.security.user.CustomUserDetailsService;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthInterceptorTest {

    @Mock
    private JwtUseCase jwtUseCase;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private UserDetails userDetails;

    @Mock
    private MessageChannel channel;

    private WebSocketAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new WebSocketAuthInterceptor(jwtUseCase, userDetailsService);
    }

    @Test
    void preSend_validToken_setsAuthenticatedUser() {
        String token = "valid.jwt.token";
        String email = "user@example.com";

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer " + token);
        accessor.setLeaveMutable(true);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtUseCase.extractEmail(token)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtUseCase.isTokenValid(token, userDetails)).thenReturn(true);
        when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());

        Message<?> result = interceptor.preSend(message, channel);

        assertNotNull(result);
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertNotNull(resultAccessor.getUser());
        assertTrue(resultAccessor.getUser() instanceof UsernamePasswordAuthenticationToken);
    }

    @Test
    void preSend_validTokenInTokenHeader_setsAuthenticatedUser() {
        String token = "valid.jwt.token";
        String email = "user@example.com";

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("token", token);
        accessor.setLeaveMutable(true);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtUseCase.extractEmail(token)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtUseCase.isTokenValid(token, userDetails)).thenReturn(true);
        when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());

        Message<?> result = interceptor.preSend(message, channel);

        assertNotNull(result);
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertNotNull(resultAccessor.getUser());
        assertTrue(resultAccessor.getUser() instanceof UsernamePasswordAuthenticationToken);
    }

    @Test
    void preSend_missingToken_throwsMessageDeliveryException() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        MessageDeliveryException exception = assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(message, channel));

        assertTrue(exception.getMessage().contains("Missing JWT token"));
    }

    @Test
    void preSend_expiredToken_throwsMessageDeliveryException() {
        String token = "expired.jwt.token";

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer " + token);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtUseCase.extractEmail(token)).thenThrow(new ExpiredJwtException(null, null, "Token expired"));

        MessageDeliveryException exception = assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(message, channel));

        assertTrue(exception.getMessage().contains("Expired JWT token"));
    }

    @Test
    void preSend_malformedToken_throwsMessageDeliveryException() {
        String token = "malformed-token";

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer " + token);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtUseCase.extractEmail(token)).thenThrow(new MalformedJwtException("Malformed"));

        MessageDeliveryException exception = assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(message, channel));

        assertTrue(exception.getMessage().contains("Malformed JWT token"));
    }

    @Test
    void preSend_invalidToken_throwsMessageDeliveryException() {
        String token = "invalid.jwt.token";
        String email = "user@example.com";

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer " + token);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtUseCase.extractEmail(token)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
        when(jwtUseCase.isTokenValid(token, userDetails)).thenReturn(false);

        MessageDeliveryException exception = assertThrows(MessageDeliveryException.class,
                () -> interceptor.preSend(message, channel));

        assertTrue(exception.getMessage().contains("Invalid JWT token"));
    }

    @Test
    void preSend_nonConnectCommand_passesThrough() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, channel);

        assertEquals(message, result);
    }

    @Test
    void preSend_subscribeCommand_passesThrough() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, channel);

        assertEquals(message, result);
    }
}
