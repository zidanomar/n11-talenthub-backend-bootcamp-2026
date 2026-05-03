package com.example.payment.dto;

import java.math.BigDecimal;

public record BasketItem(
        String id,
        String name,
        String category,
        BigDecimal price
) {}
