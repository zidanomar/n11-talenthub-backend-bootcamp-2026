package com.example.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record BuyerInfo(
        @NotBlank String id,
        @NotBlank String name,
        @NotBlank String surname,
        @NotBlank String email,
        @NotBlank String phone,
        @NotBlank String identityNumber,
        @NotBlank String addressLine,
        @NotBlank String city,
        @NotBlank String country,
        @NotBlank String zipCode,
        String ip
) {}
