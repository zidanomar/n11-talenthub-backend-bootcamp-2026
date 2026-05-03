package com.example.notification.service.impl;

import com.example.notification.dto.NotificationResponse;
import com.example.notification.entity.Notification;
import com.example.notification.repository.NotificationRepository;
import com.example.notification.service.NotificationService;
import com.example.notification.sse.SseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository repository;
    private final SseEmitterRegistry registry;

    @Override
    @Transactional
    public NotificationResponse createAndPush(Notification notification) {
        Notification saved = repository.save(notification);
        NotificationResponse dto = NotificationResponse.from(saved);
        String userId = saved.getUserId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                registry.push(userId, dto);
            }
        });
        log.info("Notification created type={} orderId={} userId={}", saved.getType(), saved.getOrderId(), userId);
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getAllForUser(String userId, LocalDate from, LocalDate to, int page, int limit) {
        LocalDateTime fromDate = from == null ? null : from.atStartOfDay();
        LocalDateTime toDate = to == null ? null : to.plusDays(1).atStartOfDay();
        var pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(limit, 1), 50),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return repository.findByUserIdAndCreatedAtRange(userId, fromDate, toDate, pageable)
                .map(NotificationResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getAllForUser(String userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(NotificationResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadForUser(String userId) {
        return repository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId)
                .stream().map(NotificationResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(String userId) {
        return repository.countByUserIdAndReadFalse(userId);
    }

    @Override
    @Transactional
    public NotificationResponse markRead(String userId, Long notificationId) {
        Notification notification = repository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!notification.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your notification");
        }
        notification.setRead(true);
        return NotificationResponse.from(repository.save(notification));
    }

    @Override
    @Transactional
    public void markAllRead(String userId) {
        List<Notification> unread = repository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
        unread.forEach(n -> n.setRead(true));
        repository.saveAll(unread);
    }
}
