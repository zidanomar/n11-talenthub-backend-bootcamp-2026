package com.example.cart.controller;

import com.example.cart.dto.CartItemRequest;
import com.example.cart.dto.CartItemResponse;
import com.example.cart.dto.CartResponse;
import com.example.cart.exception.RestExceptionHandler;
import com.example.cart.service.CartService;
import com.example.lib.exception.InsufficientStockException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
@Import(RestExceptionHandler.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
@DisplayName("CartController")
class CartControllerTest {

    private static final String USER = "user-1";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    private static CartResponse emptyCart(String userId) {
        return new CartResponse(userId, List.of(), BigDecimal.ZERO);
    }

    private static CartResponse cartWith(String userId, CartItemResponse... items) {
        BigDecimal total = List.of(items).stream()
                .map(CartItemResponse::totalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(userId, List.of(items), total);
    }

    private static CartItemResponse line(long id, String name, String price, int qty) {
        BigDecimal unit = new BigDecimal(price);
        return new CartItemResponse(id, name, unit, qty,
                unit.multiply(BigDecimal.valueOf(qty)));
    }

    // ───────────────────────── GET /api/cart ─────────────────────────

    @Nested
    @DisplayName("GET /api/cart")
    class GetCart {

        @Test
        @DisplayName("200 with full body")
        void ok() throws Exception {
            when(cartService.getCart(USER))
                    .thenReturn(cartWith(USER, line(1L, "Widget", "50.00", 2)));

            mockMvc.perform(get("/api/cart").header("X-User-Id", USER))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(USER))
                    .andExpect(jsonPath("$.items.length()").value(1))
                    .andExpect(jsonPath("$.items[0].productId").value(1))
                    .andExpect(jsonPath("$.items[0].name").value("Widget"))
                    .andExpect(jsonPath("$.items[0].unitPrice").value(50.00))
                    .andExpect(jsonPath("$.items[0].quantity").value(2))
                    .andExpect(jsonPath("$.items[0].totalPrice").value(100.00))
                    .andExpect(jsonPath("$.grandTotal").value(100.00));
        }

        @Test
        @DisplayName("empty cart → 200 with empty items")
        void empty() throws Exception {
            when(cartService.getCart(USER)).thenReturn(emptyCart(USER));

            mockMvc.perform(get("/api/cart").header("X-User-Id", USER))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isArray())
                    .andExpect(jsonPath("$.items").isEmpty())
                    .andExpect(jsonPath("$.grandTotal").value(0));
        }

        @Test
        @DisplayName("missing X-User-Id → 400")
        void missingHeader() throws Exception {
            mockMvc.perform(get("/api/cart"))
                    .andExpect(status().isBadRequest());
            verify(cartService, never()).getCart(any());
        }

        @Test
        @DisplayName("service throws → 500")
        void serviceError() throws Exception {
            when(cartService.getCart(USER)).thenThrow(new RuntimeException("boom"));

            mockMvc.perform(get("/api/cart").header("X-User-Id", USER))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value("Internal server error"));
        }
    }

    // ───────────────────────── POST /api/cart/items ─────────────────────────

    @Nested
    @DisplayName("POST /api/cart/items")
    class AddItem {

        @Test
        @DisplayName("valid → 200 with refreshed cart")
        void valid() throws Exception {
            when(cartService.addItem(eq(USER), any(CartItemRequest.class)))
                    .thenReturn(cartWith(USER, line(1L, "A", "10.00", 2)));

            mockMvc.perform(post("/api/cart/items")
                            .header("X-User-Id", USER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"productId\":1,\"quantity\":2}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items[0].productId").value(1))
                    .andExpect(jsonPath("$.grandTotal").value(20.00));
        }

        @Test
        @DisplayName("insufficient stock → 422 with message")
        void insufficientStock() throws Exception {
            when(cartService.addItem(eq(USER), any(CartItemRequest.class)))
                    .thenThrow(new InsufficientStockException(1L, 5, 2));

            mockMvc.perform(post("/api/cart/items")
                            .header("X-User-Id", USER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"productId\":1,\"quantity\":5}"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.status").value(422))
                    .andExpect(jsonPath("$.message").value(
                            "Product 1 only has 2 in stock, requested 5"));
        }

        @Test
        @DisplayName("quantity=0 → 400 validation error")
        void zeroQuantity() throws Exception {
            mockMvc.perform(post("/api/cart/items")
                            .header("X-User-Id", USER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"productId\":1,\"quantity\":0}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.errors[0].field").value("quantity"));
            verify(cartService, never()).addItem(any(), any());
        }

        @Test
        @DisplayName("negative quantity → 400 validation error")
        void negativeQuantity() throws Exception {
            mockMvc.perform(post("/api/cart/items")
                            .header("X-User-Id", USER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"productId\":1,\"quantity\":-3}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("quantity"));
        }

        @Test
        @DisplayName("missing productId → 400 validation error")
        void missingProductId() throws Exception {
            mockMvc.perform(post("/api/cart/items")
                            .header("X-User-Id", USER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"quantity\":2}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("productId"));
        }

        @Test
        @DisplayName("malformed JSON → 400")
        void malformedJson() throws Exception {
            mockMvc.perform(post("/api/cart/items")
                            .header("X-User-Id", USER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{not-json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Malformed or missing request body"));
        }

        @Test
        @DisplayName("missing body → 400")
        void missingBody() throws Exception {
            mockMvc.perform(post("/api/cart/items")
                            .header("X-User-Id", USER)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("missing X-User-Id → 400")
        void missingHeader() throws Exception {
            mockMvc.perform(post("/api/cart/items")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"productId\":1,\"quantity\":1}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ───────────────────────── DELETE /api/cart/items/{id} ─────────────────────────

    @Nested
    @DisplayName("DELETE /api/cart/items/{productId}")
    class RemoveItem {

        @Test
        @DisplayName("200 with refreshed cart")
        void ok() throws Exception {
            when(cartService.removeItem(USER, 1L)).thenReturn(emptyCart(USER));

            mockMvc.perform(delete("/api/cart/items/1").header("X-User-Id", USER))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items").isEmpty());
            verify(cartService).removeItem(USER, 1L);
        }

        @Test
        @DisplayName("non-numeric productId → 400")
        void nonNumeric() throws Exception {
            mockMvc.perform(delete("/api/cart/items/abc").header("X-User-Id", USER))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(
                            org.hamcrest.Matchers.containsString("Invalid value 'abc'")));
            verify(cartService, never()).removeItem(any(), any());
        }

        @Test
        @DisplayName("missing X-User-Id → 400")
        void missingHeader() throws Exception {
            mockMvc.perform(delete("/api/cart/items/1"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ───────────────────────── DELETE /api/cart ─────────────────────────

    @Nested
    @DisplayName("DELETE /api/cart")
    class ClearCart {

        @Test
        @DisplayName("204 No Content")
        void ok() throws Exception {
            doNothing().when(cartService).clearCart(USER);

            mockMvc.perform(delete("/api/cart").header("X-User-Id", USER))
                    .andExpect(status().isNoContent());
            verify(cartService).clearCart(USER);
        }

        @Test
        @DisplayName("missing X-User-Id → 400")
        void missingHeader() throws Exception {
            mockMvc.perform(delete("/api/cart"))
                    .andExpect(status().isBadRequest());
            verify(cartService, never()).clearCart(any());
        }

        @Test
        @DisplayName("service error → 500")
        void serviceError() throws Exception {
            doThrow(new RuntimeException("redis down"))
                    .when(cartService).clearCart(USER);

            mockMvc.perform(delete("/api/cart").header("X-User-Id", USER))
                    .andExpect(status().isInternalServerError());
        }
    }
}
