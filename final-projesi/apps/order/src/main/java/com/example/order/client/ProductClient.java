package com.example.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@FeignClient(name = "product")
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    ProductResponse getProduct(@PathVariable Long id);

    @GetMapping("/api/products")
    ProductsPage getProducts(@RequestParam List<Long> ids);

    @PatchMapping("/api/products/{id}/stock")
    void deductStock(@PathVariable Long id, @RequestBody DeductStockRequest request);

    record ProductResponse(Long id, String name, String category, BigDecimal price, Integer stock) {}
    record ProductsPage(List<ProductResponse> content) {}
    record DeductStockRequest(int quantity) {}
    record RestoreStockRequest(int quantity) {}

    @PatchMapping("/api/products/{id}/stock/restore")
    void restoreStock(@PathVariable Long id, @RequestBody RestoreStockRequest request);
}
