package com.example.user.dto;

import com.example.user.entity.Address;
import com.example.user.entity.Role;
import com.example.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        Role role,
        String phone,
        String identityNumber,
        String addressLine,
        String city,
        String country,
        String zipCode,
        Long currentAddressId,
        List<AddressResponse> addresses,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user, Address currentAddress, List<AddressResponse> addresses) {
        String phone = currentAddress != null ? currentAddress.getPhone() : null;
        String identityNumber = currentAddress != null ? currentAddress.getIdentityNumber() : null;
        String addressLine = currentAddress != null ? currentAddress.getAddressLine() : null;
        String city = currentAddress != null ? currentAddress.getCity() : null;
        String country = currentAddress != null ? currentAddress.getCountry() : null;
        String zipCode = currentAddress != null ? currentAddress.getZipCode() : null;
        return new UserResponse(
                user.getId(), user.getUsername(), user.getEmail(),
                user.getFirstName(), user.getLastName(),
                user.getRole(),
                phone, identityNumber, addressLine, city, country, zipCode,
                user.getCurrentAddress() != null ? user.getCurrentAddress().getId() : null,
                addresses,
                user.getCreatedAt()
        );
    }
}
