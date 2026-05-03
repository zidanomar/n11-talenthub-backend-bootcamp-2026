package com.example.order.dto;

public record PayOrderResponse(OrderResponse order, String paymentPageUrl, String token) {}
