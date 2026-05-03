package com.example.cart.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

@FeignClient(name = "product")
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    ProductResponse getProduct(@PathVariable Long id);

    @GetMapping("/api/products")
    ProductsPage getProducts(@RequestParam List<Long> ids);

    record ProductResponse(Long id, String name, BigDecimal price, Integer stock) {}

    record ProductsPage(List<ProductResponse> content) {}
}
