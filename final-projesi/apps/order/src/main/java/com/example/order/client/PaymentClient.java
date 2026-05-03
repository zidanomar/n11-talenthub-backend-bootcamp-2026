package com.example.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.List;

@FeignClient(name = "payment")
public interface PaymentClient {

    @PostMapping("/api/payments/initiate")
    InitiateResponse initiate(@RequestBody InitiateRequest request);

    record InitiateRequest(
            Long orderId,
            String method,
            BigDecimal totalPrice,
            BuyerInfo buyer,
            List<BasketItem> items,
            String cardUserKey,
            Boolean forceThreeDS,
            Boolean paymentWithNewCardEnabled) {}

    record InitiateResponse(Long orderId, String method, String token, String paymentPageUrl) {}

    record BuyerInfo(
            String id, String name, String surname,
            String email, String phone, String identityNumber,
            String addressLine, String city, String country, String zipCode,
            String ip) {}

    record BasketItem(String id, String name, String category, BigDecimal price) {}
}
