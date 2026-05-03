package com.example.cart.repository;

import com.example.cart.entity.CartItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CartRepository {

    private static final String KEY_PREFIX = "cart:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private record StoredItem(BigDecimal unitPrice, int quantity) {}

    private String key(String userId) {
        return KEY_PREFIX + userId;
    }

    public List<CartItem> findByUserId(String userId) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key(userId));
        if (entries.isEmpty()) return Collections.emptyList();
        return entries.entrySet().stream()
                .map(e -> deserialize(
                        Long.valueOf((String) e.getKey()),
                        (String) e.getValue()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    public Optional<CartItem> findItem(String userId, Long productId) {
        Object raw = redisTemplate.opsForHash().get(key(userId), String.valueOf(productId));
        if (raw == null) return Optional.empty();
        return deserialize(productId, (String) raw);
    }

    public void save(String userId, CartItem item) {
        var stored = new StoredItem(item.unitPrice(), item.quantity());
        redisTemplate.opsForHash().put(key(userId), String.valueOf(item.productId()), serialize(stored));
    }

    public void remove(String userId, Long productId) {
        redisTemplate.opsForHash().delete(key(userId), String.valueOf(productId));
    }

    public void clear(String userId) {
        redisTemplate.delete(key(userId));
    }

    private String serialize(StoredItem item) {
        try {
            return objectMapper.writeValueAsString(item);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize cart item", e);
        }
    }

    private Optional<CartItem> deserialize(Long productId, String json) {
        try {
            var stored = objectMapper.readValue(json, StoredItem.class);
            return Optional.of(new CartItem(productId, stored.unitPrice(), stored.quantity()));
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize cart item for productId={}: {}", productId, json);
            return Optional.empty();
        }
    }
}
