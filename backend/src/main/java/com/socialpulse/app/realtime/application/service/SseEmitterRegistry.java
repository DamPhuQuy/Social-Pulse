package com.socialpulse.app.realtime.application.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SseEmitterRegistry {

    private final Map<Long, List<SseEmitter>> userEmitters = new ConcurrentHashMap<>();
    private final ExecutorService sseExecutor = Executors.newFixedThreadPool(10);

    public void register(Long userId, SseEmitter emitter) {
        userEmitters.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(e -> removeEmitter(userId, emitter));
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                userEmitters.remove(userId);
            }
        }
    }

    public void sendToUser(Long userId, String eventName, Object data) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters != null) {
            List<SseEmitter> deadEmitters = new ArrayList<>();
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().name(eventName).data(data));
                } catch (IOException e) {
                    log.debug("Failed to send SSE event to user {}, removing emitter.", userId);
                    deadEmitters.add(emitter);
                }
            }
            emitters.removeAll(deadEmitters);
            if (emitters.isEmpty()) {
                userEmitters.remove(userId);
            }
        }
    }

    public void broadcast(String eventName, Object data) {
        sseExecutor.submit(() -> {
            for (Map.Entry<Long, List<SseEmitter>> entry : userEmitters.entrySet()) {
                sendToUser(entry.getKey(), eventName, data);
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down SseEmitterRegistry thread pool...");
        sseExecutor.shutdown();
        try {
            if (!sseExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                sseExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            sseExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
