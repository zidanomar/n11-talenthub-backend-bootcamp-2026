package com.example.user.dto;

import jakarta.validation.constraints.NotBlank;

public record SigninRequest(
        @NotBlank String username,
        @NotBlank String password
) {}
