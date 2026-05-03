package com.example.cart.entity;

import java.math.BigDecimal;

public record CartItem(
        Long productId,
        BigDecimal unitPrice,
        int quantity
) {
    public BigDecimal totalPrice() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
