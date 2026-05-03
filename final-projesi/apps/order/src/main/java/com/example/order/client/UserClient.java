package com.example.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDateTime;
import java.util.List;

@FeignClient(name = "user")
public interface UserClient {

    @GetMapping("/api/users/{id}")
    UserResponse getById(@PathVariable String id);

    record UserResponse(
            String id, String username, String email,
            String firstName, String lastName,
            String phone, String identityNumber,
            String addressLine, String city, String country, String zipCode,
            Long defaultCardId,
            List<CardResponse> cards,
            LocalDateTime createdAt) {}

    record CardResponse(
            Long id,
            String label,
            String cardHolderName,
            String lastFourDigits,
            String expiryMonth,
            String expiryYear,
            String cardType,
            String iyzipayCardToken,
            String iyzipayCardUserKey,
            LocalDateTime createdAt) {}
}
