package com.example.order.dto;

import jakarta.validation.constraints.NotBlank;

public record PayOrderRequest(
        @NotBlank String method,
        Long savedCardId,
        Boolean forceThreeDS,
        Boolean paymentWithNewCardEnabled
) {}
