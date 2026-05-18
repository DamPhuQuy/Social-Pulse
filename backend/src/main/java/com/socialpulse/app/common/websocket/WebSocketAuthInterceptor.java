package com.socialpulse.app.common.websocket;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.socialpulse.app.auth.application.usecase.JwtUseCase;
import com.socialpulse.app.security.user.CustomUserDetailsService;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;

/**
 * Intercepts STOMP CONNECT frames to authenticate WebSocket connections via JWT.
 */
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUseCase jwtUseCase;
    private final CustomUserDetailsService userDetailsService;

    public WebSocketAuthInterceptor(JwtUseCase jwtUseCase, CustomUserDetailsService userDetailsService) {
        this.jwtUseCase = jwtUseCase;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        String token = resolveToken(accessor);
        if (token == null || token.isBlank()) {
            throw new MessageDeliveryException("Authentication failed: Missing JWT token");
        }

        try {
            String email = jwtUseCase.extractEmail(token);
            if (email == null) {
                throw new MessageDeliveryException("Authentication failed: Malformed JWT token");
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            if (!jwtUseCase.isTokenValid(token, userDetails)) {
                throw new MessageDeliveryException("Authentication failed: Invalid JWT token");
            }

            accessor.setUser(new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()));

        } catch (ExpiredJwtException e) {
            throw new MessageDeliveryException("Authentication failed: Expired JWT token");
        } catch (MalformedJwtException e) {
            throw new MessageDeliveryException("Authentication failed: Malformed JWT token");
        } catch (SignatureException e) {
            throw new MessageDeliveryException("Authentication failed: Invalid JWT token signature");
        } catch (MessageDeliveryException e) {
            throw e;
        } catch (Exception e) {
            throw new MessageDeliveryException("Authentication failed: " + e.getMessage());
        }

        return message;
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return accessor.getFirstNativeHeader("token");
    }
}
