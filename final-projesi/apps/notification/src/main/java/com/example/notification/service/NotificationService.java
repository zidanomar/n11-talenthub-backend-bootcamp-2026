package com.example.notification.service;

import com.example.notification.dto.NotificationResponse;
import com.example.notification.entity.Notification;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

public interface NotificationService {
    NotificationResponse createAndPush(Notification notification);
    Page<NotificationResponse> getAllForUser(String userId, LocalDate from, LocalDate to, int page, int limit);
    List<NotificationResponse> getAllForUser(String userId);
    List<NotificationResponse> getUnreadForUser(String userId);
    long countUnread(String userId);
    NotificationResponse markRead(String userId, Long notificationId);
    void markAllRead(String userId);
}
