package com.example.cart.controller;

import com.example.cart.dto.CartItemRequest;
import com.example.cart.dto.CartResponse;
import com.example.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(@RequestHeader("X-User-Id") String userId,
                                                @Valid @RequestBody CartItemRequest request) {
        return ResponseEntity.ok(cartService.addItem(userId, request));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartResponse> removeItem(@RequestHeader("X-User-Id") String userId,
                                                   @PathVariable Long productId) {
        return ResponseEntity.ok(cartService.removeItem(userId, productId));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@RequestHeader("X-User-Id") String userId) {
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}
