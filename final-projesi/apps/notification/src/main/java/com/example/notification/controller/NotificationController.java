package com.example.notification.controller;

import com.example.notification.dto.NotificationResponse;
import com.example.notification.service.NotificationService;
import com.example.notification.sse.SseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SseEmitterRegistry registry;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestHeader("X-User-Id") String userId) {
        SseEmitter emitter = registry.register(userId);
        List<NotificationResponse> unread = notificationService.getUnreadForUser(userId);
        unread.forEach(n -> {
            try {
                emitter.send(SseEmitter.event().name("notification").data(n));
            } catch (IOException e) {
                log.debug("Failed to send initial unread to userId={}", userId);
            }
        });
        log.info("SSE stream connected userId={} unreadFlushed={}", userId, unread.size());
        return emitter;
    }

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getAll(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(notificationService.getAllForUser(userId, from, to, page, limit));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(Map.of("count", notificationService.countUnread(userId)));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markRead(@RequestHeader("X-User-Id") String userId,
                                                         @PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markRead(userId, id));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@RequestHeader("X-User-Id") String userId) {
        notificationService.markAllRead(userId);
        return ResponseEntity.noContent().build();
    }
}
