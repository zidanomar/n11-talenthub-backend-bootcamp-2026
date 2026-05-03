package com.example.payment.dto;

import com.example.payment.provider.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record InitiatePaymentRequest(
        @NotNull Long orderId,
        @NotNull PaymentMethod method,
        @NotNull @Positive BigDecimal totalPrice,
        @NotNull @Valid BuyerInfo buyer,
        @NotEmpty List<@Valid BasketItem> items,
        String cardUserKey,
        Boolean forceThreeDS,
        Boolean paymentWithNewCardEnabled
) {}
