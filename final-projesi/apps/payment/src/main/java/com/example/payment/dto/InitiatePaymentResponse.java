package com.example.payment.dto;

import com.example.payment.provider.PaymentMethod;

public record InitiatePaymentResponse(
        Long orderId,
        PaymentMethod method,
        String token,
        String paymentPageUrl
) {}
