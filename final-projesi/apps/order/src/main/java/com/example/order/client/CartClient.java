package com.example.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.math.BigDecimal;
import java.util.List;

@FeignClient(name = "cart")
public interface CartClient {

    @GetMapping("/api/cart")
    CartResponse getCart(@RequestHeader("X-User-Id") String userId);

    @DeleteMapping("/api/cart")
    void clearCart(@RequestHeader("X-User-Id") String userId);

    record CartResponse(String userId, List<CartItem> items, BigDecimal grandTotal) {}
    record CartItem(Long productId, BigDecimal unitPrice, int quantity) {}
}
