package com.example.order.dto;

import com.example.order.entity.Order;
import com.example.order.entity.OrderStatus;
import com.example.order.entity.OrderStatusHistory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public record OrderResponse(
        Long id,
        String userId,
        OrderStatus status,
        List<OrderItemResponse> items,
        BigDecimal totalPrice,
        LocalDateTime createdAt,
        List<StatusHistoryEntry> statusHistory
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getStatus(),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                order.getTotalPrice(),
                order.getCreatedAt(),
                order.getStatusHistory().stream()
                        .sorted(Comparator.comparing(
                                OrderStatusHistory::getChangedAt,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        ))
                        .map(StatusHistoryEntry::from)
                        .toList()
        );
    }
}
