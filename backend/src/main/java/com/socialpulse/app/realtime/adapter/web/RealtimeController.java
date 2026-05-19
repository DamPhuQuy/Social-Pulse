package com.socialpulse.app.realtime.adapter.web;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.socialpulse.app.realtime.application.service.SseEmitterRegistry;
import com.socialpulse.app.security.user.CustomUserDetails;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/realtime")
@Tag(name = "Realtime", description = "Server-Sent Events (SSE) APIs")
@Slf4j
public class RealtimeController {

    private final SseEmitterRegistry sseEmitterRegistry;

    public RealtimeController(SseEmitterRegistry sseEmitterRegistry) {
        this.sseEmitterRegistry = sseEmitterRegistry;
    }

    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Connect to Realtime SSE stream")
    public SseEmitter connect(@AuthenticationPrincipal CustomUserDetails currentUser) {
        // Timeout 30 minutes
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        sseEmitterRegistry.register(currentUser.getId(), emitter);

        try {
            // Send initial connection event
            emitter.send(SseEmitter.event().name("connected").data("connected successfully"));
        } catch (IOException e) {
            log.error("Error sending initial SSE event", e);
            emitter.completeWithError(e);
        }

        return emitter;
    }
}
