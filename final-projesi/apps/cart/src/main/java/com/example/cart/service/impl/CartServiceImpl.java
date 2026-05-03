package com.example.cart.service.impl;

import com.example.cart.client.ProductClient;
import com.example.cart.dto.CartItemRequest;
import com.example.cart.dto.CartItemResponse;
import com.example.cart.dto.CartResponse;
import com.example.cart.entity.CartItem;
import com.example.cart.repository.CartRepository;
import com.example.cart.service.CartService;
import com.example.lib.exception.InsufficientStockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductClient productClient;

    @Override
    public CartResponse getCart(String userId) {
        log.debug("getCart userId={}", userId);
        List<CartItem> cartItems = cartRepository.findByUserId(userId);
        if (cartItems.isEmpty()) {
            return CartResponse.of(userId, List.of());
        }
        List<Long> ids = cartItems.stream().map(CartItem::productId).toList();
        Map<Long, ProductClient.ProductResponse> products = productClient.getProducts(ids).content().stream()
                .collect(Collectors.toMap(ProductClient.ProductResponse::id, Function.identity()));
        List<CartItemResponse> items = cartItems.stream()
                .map(item -> {
                    var product = products.get(item.productId());
                    return new CartItemResponse(
                            item.productId(),
                            product != null ? product.name() : null,
                            item.unitPrice(),
                            item.quantity(),
                            item.totalPrice()
                    );
                })
                .toList();
        return CartResponse.of(userId, items);
    }

    @Override
    public CartResponse addItem(String userId, CartItemRequest request) {
        log.debug("addItem userId={} productId={} qty={}", userId, request.productId(), request.quantity());
        var product = productClient.getProduct(request.productId());
        if (request.quantity() > product.stock()) {
            throw new InsufficientStockException(request.productId(), request.quantity(), product.stock());
        }
        var item = new CartItem(request.productId(), product.price(), request.quantity());
        cartRepository.save(userId, item);
        return getCart(userId);
    }

    @Override
    public CartResponse removeItem(String userId, Long productId) {
        log.debug("removeItem userId={} productId={}", userId, productId);
        cartRepository.remove(userId, productId);
        return getCart(userId);
    }

    @Override
    public void clearCart(String userId) {
        log.debug("clearCart userId={}", userId);
        cartRepository.clear(userId);
    }
}
