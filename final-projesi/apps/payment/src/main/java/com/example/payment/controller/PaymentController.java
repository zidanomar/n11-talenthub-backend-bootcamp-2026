package com.example.payment.controller;

import com.example.payment.dto.InitiatePaymentRequest;
import com.example.payment.dto.InitiatePaymentResponse;
import com.example.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${payment.frontend-order-url:http://localhost:8080/orders}")
    private String frontendOrderUrl;

    @PostMapping("/initiate")
    public ResponseEntity<InitiatePaymentResponse> initiate(@Valid @RequestBody InitiatePaymentRequest request) {
        return ResponseEntity.ok(paymentService.initiate(request));
    }

    @PostMapping("/callback/{method}")
    public ResponseEntity<Void> callback(
            @PathVariable String method,
            @RequestParam String token,
            @RequestParam(required = false) Long orderId) {
        try {
            var result = paymentService.handleCallback(method, token, orderId);
            return redirectToOrders(result.accepted() ? "success" : "failed", result.orderId(), result.reason());
        } catch (Exception e) {
            log.error("Payment callback error method={} token={} orderId={}", method, token, orderId, e);
            return redirectToOrders("failed", orderId, "callback_error");
        }
    }

    private ResponseEntity<Void> redirectToOrders(String payment, Long orderId, String reason) {
        var builder = UriComponentsBuilder.fromUriString(frontendOrderUrl)
                .queryParam("payment", payment);
        if (orderId != null) {
            builder.queryParam("orderId", orderId);
        }
        if (reason != null && !reason.isBlank()) {
            builder.queryParam("reason", reason);
        }
        return ResponseEntity.status(302)
                .location(URI.create(builder.build().encode().toUriString()))
                .build();
    }
}
