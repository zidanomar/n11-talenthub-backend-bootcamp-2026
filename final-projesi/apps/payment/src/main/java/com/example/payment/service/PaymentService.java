package com.example.payment.service;

import com.example.payment.dto.InitiatePaymentRequest;
import com.example.payment.dto.InitiatePaymentResponse;
import com.example.payment.dto.VerifyPaymentResult;

public interface PaymentService {
    InitiatePaymentResponse initiate(InitiatePaymentRequest request);
    VerifyPaymentResult handleCallback(String method, String token, Long callbackOrderId);
    void processRefund(Long orderId);
}
