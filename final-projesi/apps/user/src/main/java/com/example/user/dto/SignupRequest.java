package com.example.user.dto;

import com.example.user.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank String username,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6) String password,
        @NotBlank String firstName,
        @NotBlank String lastName,
        Role role
) {}
