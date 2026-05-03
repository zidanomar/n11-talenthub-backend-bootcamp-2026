package com.example.lib.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(Long productId, int requested, int available) {
        super("Product " + productId + " only has " + available + " in stock, requested " + requested);
    }
}
