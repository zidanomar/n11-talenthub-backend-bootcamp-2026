package com.example.order.controller;

import com.example.lib.exception.InsufficientStockException;
import com.example.order.dto.OrderResponse;
import com.example.order.dto.PayOrderResponse;
import com.example.order.dto.StatusHistoryEntry;
import com.example.order.entity.OrderStatus;
import com.example.order.exception.RestExceptionHandler;
import com.example.order.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import(RestExceptionHandler.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
@DisplayName("OrderController")
class OrderControllerTest {

    private static final String USER = "user-1";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    private static OrderResponse sampleOrder(Long id, OrderStatus status) {
        return new OrderResponse(id, USER, status, List.of(),
                new BigDecimal("100.00"), LocalDateTime.now(), List.of());
    }

    // ─────────────────────────── POST /api/orders ───────────────────────────

    @Nested
    @DisplayName("POST /api/orders")
    class PlaceOrder {

        @Test
        @DisplayName("201 Created with body")
        void created() throws Exception {
            when(orderService.placeOrder(eq(USER), any()))
                    .thenReturn(sampleOrder(1L, OrderStatus.PENDING));

            mockMvc.perform(post("/api/orders")
                            .header("X-User-Id", USER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.totalPrice").value(100.00));
        }

        @Test
        @DisplayName("body optional — empty POST works")
        void emptyBodyAccepted() throws Exception {
            when(orderService.placeOrder(eq(USER), any()))
                    .thenReturn(sampleOrder(1L, OrderStatus.PENDING));

            mockMvc.perform(post("/api/orders").header("X-User-Id", USER))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("empty cart → 400")
        void emptyCart() throws Exception {
            when(orderService.placeOrder(eq(USER), any()))
                    .thenThrow(new ResponseStatusException(BAD_REQUEST, "Cart is empty"));

            mockMvc.perform(post("/api/orders")
                            .header("X-User-Id", USER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Cart is empty"));
        }

        @Test
        @DisplayName("missing X-User-Id → 400")
        void missingHeader() throws Exception {
            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
            verify(orderService, never()).placeOrder(any(), any());
        }
    }

    // ─────────────────────────── GET /api/orders ────────────────────────────

    @Nested
    @DisplayName("GET /api/orders")
    class ListOrders {

        @Test
        @DisplayName("default pagination → page=0, size=6")
        void defaultPagination() throws Exception {
            when(orderService.getOrdersByUser(USER, null, null, 0, 6))
                    .thenReturn(new PageImpl<>(List.of(
                            sampleOrder(1L, OrderStatus.PENDING),
                            sampleOrder(2L, OrderStatus.PAID))));

            mockMvc.perform(get("/api/orders").header("X-User-Id", USER))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[0].id").value(1));
        }

        @Test
        @DisplayName("custom page + size + date range passed through")
        void customParams() throws Exception {
            when(orderService.getOrdersByUser(eq(USER), any(), any(), eq(2), eq(12)))
                    .thenReturn(new PageImpl<>(List.of(sampleOrder(3L, OrderStatus.PAID))));

            mockMvc.perform(get("/api/orders")
                            .header("X-User-Id", USER)
                            .param("page", "2")
                            .param("size", "12")
                            .param("from", "2026-01-01")
                            .param("to", "2026-12-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(3));
        }

        @Test
        @DisplayName("invalid date format → 400")
        void invalidDate() throws Exception {
            mockMvc.perform(get("/api/orders")
                            .header("X-User-Id", USER)
                            .param("from", "not-a-date"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("missing header → 400")
        void missingHeader() throws Exception {
            mockMvc.perform(get("/api/orders"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────── GET /api/orders/{id} ───────────────────────

    @Nested
    @DisplayName("GET /api/orders/{id}")
    class GetOrder {

        @Test
        @DisplayName("200 with order body")
        void ok() throws Exception {
            when(orderService.getOrderById(USER, 1L)).thenReturn(sampleOrder(1L, OrderStatus.PAID));

            mockMvc.perform(get("/api/orders/1").header("X-User-Id", USER))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.status").value("PAID"));
        }

        @Test
        @DisplayName("not found → 404")
        void notFound() throws Exception {
            when(orderService.getOrderById(USER, 99L))
                    .thenThrow(new ResponseStatusException(NOT_FOUND, "Order not found"));

            mockMvc.perform(get("/api/orders/99").header("X-User-Id", USER))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("wrong owner → 403")
        void forbidden() throws Exception {
            when(orderService.getOrderById(USER, 1L))
                    .thenThrow(new ResponseStatusException(FORBIDDEN, "Order not owned by user"));

            mockMvc.perform(get("/api/orders/1").header("X-User-Id", USER))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("non-numeric id → 400")
        void badPathParam() throws Exception {
            mockMvc.perform(get("/api/orders/abc").header("X-User-Id", USER))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────── GET /api/orders/{id}/history ───────────────

    @Nested
    @DisplayName("GET /api/orders/{id}/history")
    class History {

        @Test
        @DisplayName("returns history entries")
        void ok() throws Exception {
            when(orderService.getOrderHistory(USER, 1L)).thenReturn(List.of(
                    new StatusHistoryEntry(OrderStatus.PENDING, LocalDateTime.now(), "Order placed"),
                    new StatusHistoryEntry(OrderStatus.PAID, LocalDateTime.now(), "Payment accepted")));

            mockMvc.perform(get("/api/orders/1/history").header("X-User-Id", USER))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].status").value("PENDING"))
                    .andExpect(jsonPath("$[1].status").value("PAID"));
        }
    }

    // ─────────────────────────── POST /api/orders/{id}/pay ──────────────────

    @Nested
    @DisplayName("POST /api/orders/{id}/pay")
    class Pay {

        @Test
        @DisplayName("202 Accepted with payment url + token")
        void accepted() throws Exception {
            when(orderService.payOrder(eq(USER), eq(1L), any()))
                    .thenReturn(new PayOrderResponse(
                            sampleOrder(1L, OrderStatus.PENDING),
                            "https://pay.example/", "tok-123"));

            mockMvc.perform(post("/api/orders/1/pay")
                            .header("X-User-Id", USER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"method\":\"IYZICO\"}"))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.paymentPageUrl").value("https://pay.example/"))
                    .andExpect(jsonPath("$.token").value("tok-123"))
                    .andExpect(jsonPath("$.order.id").value(1));
        }

        @Test
        @DisplayName("insufficient stock → 422")
        void insufficientStock() throws Exception {
            when(orderService.payOrder(eq(USER), eq(1L), any()))
                    .thenThrow(new InsufficientStockException(1L, 5, 2));

            mockMvc.perform(post("/api/orders/1/pay")
                            .header("X-User-Id", USER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"method\":\"IYZICO\"}"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message").value(
                            "Product 1 only has 2 in stock, requested 5"));
        }

        @Test
        @DisplayName("not found → 404")
        void notFound() throws Exception {
            when(orderService.payOrder(eq(USER), eq(99L), any()))
                    .thenThrow(new ResponseStatusException(NOT_FOUND, "Order not found"));

            mockMvc.perform(post("/api/orders/99/pay")
                            .header("X-User-Id", USER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"method\":\"IYZICO\"}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("status conflict → 409")
        void conflict() throws Exception {
            when(orderService.payOrder(eq(USER), eq(1L), any()))
                    .thenThrow(new ResponseStatusException(CONFLICT, "Order must be PENDING or PAYMENT_FAILED to pay, current=PAID"));

            mockMvc.perform(post("/api/orders/1/pay")
                            .header("X-User-Id", USER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"method\":\"IYZICO\"}"))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("blank method → 400 validation")
        void blankMethod() throws Exception {
            mockMvc.perform(post("/api/orders/1/pay")
                            .header("X-User-Id", USER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"method\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("method"));
        }

        @Test
        @DisplayName("missing body → 400")
        void missingBody() throws Exception {
            mockMvc.perform(post("/api/orders/1/pay")
                            .header("X-User-Id", USER)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("malformed JSON → 400")
        void malformedJson() throws Exception {
            mockMvc.perform(post("/api/orders/1/pay")
                            .header("X-User-Id", USER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("not-json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Malformed or missing request body"));
        }
    }

    // ─────────────────────────── POST /api/orders/{id}/cancel ───────────────

    @Nested
    @DisplayName("POST /api/orders/{id}/cancel")
    class Cancel {

        @Test
        @DisplayName("200 with cancelled order")
        void ok() throws Exception {
            when(orderService.cancelOrderByUser(USER, 1L))
                    .thenReturn(sampleOrder(1L, OrderStatus.CANCELLED));

            mockMvc.perform(post("/api/orders/1/cancel").header("X-User-Id", USER))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        @Test
        @DisplayName("paid → 409")
        void paidConflict() throws Exception {
            when(orderService.cancelOrderByUser(USER, 1L))
                    .thenThrow(new ResponseStatusException(CONFLICT, "Only PENDING or PAYMENT_FAILED orders can be cancelled, current=PAID"));

            mockMvc.perform(post("/api/orders/1/cancel").header("X-User-Id", USER))
                    .andExpect(status().isConflict());
        }
    }

    // ─────────────────────────── POST /api/orders/{id}/return ───────────────

    @Nested
    @DisplayName("POST /api/orders/{id}/return")
    class Return {

        @Test
        @DisplayName("202 Accepted")
        void accepted() throws Exception {
            when(orderService.initiateReturn(USER, 1L))
                    .thenReturn(sampleOrder(1L, OrderStatus.RETURNING));

            mockMvc.perform(post("/api/orders/1/return").header("X-User-Id", USER))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.status").value("RETURNING"));
        }
    }

    // ─────────────────────────── POST /api/orders/{id}/finish ───────────────

    @Nested
    @DisplayName("POST /api/orders/{id}/finish")
    class Finish {

        @Test
        @DisplayName("200 OK")
        void ok() throws Exception {
            when(orderService.finishOrder(USER, 1L))
                    .thenReturn(sampleOrder(1L, OrderStatus.COMPLETED));

            mockMvc.perform(post("/api/orders/1/finish").header("X-User-Id", USER))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }
    }

    // ─────────────────────────── admin routes ───────────────────────────────

    @Nested
    @DisplayName("Admin endpoints")
    class Admin {

        @Test
        @DisplayName("GET /admin without merchant role → 403")
        void getAdminForbidden() throws Exception {
            mockMvc.perform(get("/api/orders/admin").header("X-User-Roles", "USER"))
                    .andExpect(status().isForbidden());
            verify(orderService, never()).getOrders(any(), any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
        }

        @Test
        @DisplayName("GET /admin with MERCHANT role → 200")
        void getAdminOk() throws Exception {
            when(orderService.getOrders(null, null, 0, 12))
                    .thenReturn(new PageImpl<>(List.of(sampleOrder(1L, OrderStatus.PAID))));

            mockMvc.perform(get("/api/orders/admin").header("X-User-Roles", "MERCHANT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(1));
        }

        @Test
        @DisplayName("GET /admin with multi-role list including MERCHANT → 200")
        void multiRole() throws Exception {
            when(orderService.getOrders(null, null, 0, 12))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/orders/admin").header("X-User-Roles", "USER, MERCHANT, OPS"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /admin with no role header → 403")
        void noRoleHeader() throws Exception {
            mockMvc.perform(get("/api/orders/admin"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PATCH /admin/{id}/status null status → 400")
        void patchNullStatus() throws Exception {
            mockMvc.perform(patch("/api/orders/admin/1/status")
                            .header("X-User-Roles", "MERCHANT")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("PATCH /admin/{id}/status with valid status → 200")
        void patchOk() throws Exception {
            when(orderService.updateStatusForAdmin(1L, OrderStatus.SHIPPED))
                    .thenReturn(sampleOrder(1L, OrderStatus.SHIPPED));

            mockMvc.perform(patch("/api/orders/admin/1/status")
                            .header("X-User-Roles", "MERCHANT")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"SHIPPED\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SHIPPED"));
        }
    }
}
