package com.example.payment.dto;

public record VerifyPaymentResult(
        Long orderId,
        boolean accepted,
        String reason,
        String providerPaymentId
) {
    public VerifyPaymentResult(Long orderId, boolean accepted, String reason) {
        this(orderId, accepted, reason, null);
    }
}
