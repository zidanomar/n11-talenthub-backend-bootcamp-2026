package com.example.cart.dto;

import java.math.BigDecimal;

public record CartItemResponse(
        Long productId,
        String name,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal totalPrice
) {}
