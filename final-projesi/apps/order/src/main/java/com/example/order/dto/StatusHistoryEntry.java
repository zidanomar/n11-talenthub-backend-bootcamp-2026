package com.example.order.dto;

import com.example.order.entity.OrderStatus;
import com.example.order.entity.OrderStatusHistory;

import java.time.LocalDateTime;

public record StatusHistoryEntry(
        OrderStatus status,
        LocalDateTime changedAt,
        String note
) {
    public static StatusHistoryEntry from(OrderStatusHistory h) {
        return new StatusHistoryEntry(h.getStatus(), h.getChangedAt(), h.getNote());
    }
}
