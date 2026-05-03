package com.example.payment.provider;

import com.example.payment.dto.InitiatePaymentRequest;
import com.example.payment.dto.InitiatePaymentResponse;
import com.example.payment.dto.VerifyPaymentResult;

public interface PaymentProvider {

    PaymentMethod method();

    InitiatePaymentResponse initiate(InitiatePaymentRequest request);

    VerifyPaymentResult verify(String token);

    void refund(Long orderId, String providerPaymentId);
}
