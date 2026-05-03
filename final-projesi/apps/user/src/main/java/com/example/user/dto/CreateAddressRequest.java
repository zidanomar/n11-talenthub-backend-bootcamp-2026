package com.example.user.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAddressRequest(
        String label,
        @NotBlank String phone,
        @NotBlank String identityNumber,
        @NotBlank String addressLine,
        @NotBlank String city,
        @NotBlank String country,
        @NotBlank String zipCode
) {}
