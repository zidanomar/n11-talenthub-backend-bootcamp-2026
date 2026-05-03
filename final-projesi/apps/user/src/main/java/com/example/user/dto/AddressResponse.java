package com.example.user.dto;

import com.example.user.entity.Address;

import java.time.LocalDateTime;

public record AddressResponse(
        Long id,
        String label,
        String phone,
        String identityNumber,
        String addressLine,
        String city,
        String country,
        String zipCode,
        LocalDateTime createdAt
) {
    public static AddressResponse from(Address a) {
        return new AddressResponse(
                a.getId(), a.getLabel(), a.getPhone(), a.getIdentityNumber(),
                a.getAddressLine(), a.getCity(), a.getCountry(), a.getZipCode(),
                a.getCreatedAt()
        );
    }
}
