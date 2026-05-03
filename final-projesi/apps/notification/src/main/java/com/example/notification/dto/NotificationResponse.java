package com.example.notification.dto;

import com.example.notification.entity.Notification;
import com.example.notification.entity.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String message,
        Long orderId,
        boolean read,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getType(), n.getTitle(),
                n.getMessage(), n.getOrderId(), n.isRead(), n.getCreatedAt()
        );
    }
}
