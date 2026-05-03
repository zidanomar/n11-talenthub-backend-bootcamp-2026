package com.example.product.dto;

import java.util.List;

public record ProductFiltersResponse(
        List<String> categories,
        List<String> brands
) {}
