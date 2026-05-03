package com.example.cart.service;

import com.example.cart.dto.CartItemRequest;
import com.example.cart.dto.CartResponse;

public interface CartService {

    CartResponse getCart(String userId);

    CartResponse addItem(String userId, CartItemRequest request);

    CartResponse removeItem(String userId, Long productId);

    void clearCart(String userId);
}
