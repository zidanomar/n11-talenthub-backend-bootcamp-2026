package com.example.notification.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SseEmitterRegistry {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(String userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        SseEmitter old = emitters.put(userId, emitter);
        if (old != null) {
            old.complete();
        }
        emitter.onCompletion(() -> {
            emitters.remove(userId, emitter);
            log.debug("SSE emitter completed for userId={}", userId);
        });
        emitter.onTimeout(() -> {
            emitters.remove(userId, emitter);
            log.debug("SSE emitter timed out for userId={}", userId);
        });
        emitter.onError(e -> {
            emitters.remove(userId, emitter);
            log.debug("SSE emitter error for userId={}: {}", userId, e.getMessage());
        });
        log.debug("SSE emitter registered for userId={}", userId);
        return emitter;
    }

    public void push(String userId, Object payload) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            return;
        }
        try {
            emitter.send(SseEmitter.event().name("notification").data(payload));
        } catch (IOException e) {
            emitters.remove(userId, emitter);
            log.debug("SSE push failed for userId={}, removing emitter", userId);
        }
    }
}
