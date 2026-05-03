package com.example.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartItemRequest(
        @NotNull(message = "productId is required")
        Long productId,

        @Min(value = 1, message = "quantity must be at least 1")
        int quantity
) {}
