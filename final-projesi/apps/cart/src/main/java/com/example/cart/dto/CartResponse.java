package com.example.cart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        String userId,
        List<CartItemResponse> items,
        BigDecimal grandTotal
) {
    public static CartResponse of(String userId, List<CartItemResponse> items) {
        BigDecimal grandTotal = items.stream()
                .map(CartItemResponse::totalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(userId, items, grandTotal);
    }
}
