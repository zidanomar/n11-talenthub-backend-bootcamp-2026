package com.example.user.dto;

import com.example.user.entity.User;

import java.time.LocalDateTime;

public record SignupResponse(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        LocalDateTime createdAt
) {
    public static SignupResponse from(User user) {
        return new SignupResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getCreatedAt()
        );
    }
}
