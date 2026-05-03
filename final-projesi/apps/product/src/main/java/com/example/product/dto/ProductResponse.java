package com.example.product.dto;

import com.example.product.entity.Product;

import java.math.BigDecimal;
import java.util.List;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String category,
        String brand,
        Double rating,
        Integer reviewCount,
        List<String> images,
        Integer stock
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getPrice(),
                p.getCategory(),
                p.getBrand(),
                p.getRating(),
                p.getReviewCount(),
                List.copyOf(p.getImages()),
                p.getStock()
        );
    }
}
