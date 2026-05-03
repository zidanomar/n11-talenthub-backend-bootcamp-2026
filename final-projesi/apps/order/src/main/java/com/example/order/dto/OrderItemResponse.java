package com.example.order.dto;

import com.example.order.entity.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long productId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getProductId(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
        );
    }
}
