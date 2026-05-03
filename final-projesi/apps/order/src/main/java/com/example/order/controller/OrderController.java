package com.example.order.controller;

import com.example.order.dto.OrderResponse;
import com.example.order.dto.PayOrderRequest;
import com.example.order.dto.PayOrderResponse;
import com.example.order.dto.PlaceOrderRequest;
import com.example.order.dto.StatusHistoryEntry;
import com.example.order.entity.OrderStatus;
import com.example.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@RequestHeader("X-User-Id") String userId,
                                                    @RequestBody(required = false) PlaceOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder(userId, request));
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getOrders(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size
    ) {
        return ResponseEntity.ok(orderService.getOrdersByUser(userId, from, to, page, size));
    }

    @GetMapping("/admin")
    public ResponseEntity<Page<OrderResponse>> getAdminOrders(
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        requireMerchant(roles);

        return ResponseEntity.ok(orderService.getOrders(from, to, page, size));
    }

    @GetMapping("/admin/{id}")
    public ResponseEntity<OrderResponse> getAdminOrder(
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @PathVariable("id") Long orderId
    ) {
        requireMerchant(roles);

        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    @PatchMapping("/admin/{id}/status")
    public ResponseEntity<OrderResponse> updateAdminOrderStatus(
            @RequestHeader(value = "X-User-Roles", required = false) String roles,
            @PathVariable("id") Long orderId,
            @RequestBody UpdateOrderStatusRequest request
    ) {
        requireMerchant(roles);
        if (request == null || request.status() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order status is required");
        }

        return ResponseEntity.ok(orderService.updateStatusForAdmin(orderId, request.status()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@RequestHeader("X-User-Id") String userId,
                                                  @PathVariable("id") Long orderId) {
        return ResponseEntity.ok(orderService.getOrderById(userId, orderId));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<StatusHistoryEntry>> getOrderHistory(@RequestHeader("X-User-Id") String userId,
                                                                    @PathVariable("id") Long orderId) {
        return ResponseEntity.ok(orderService.getOrderHistory(userId, orderId));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<PayOrderResponse> payOrder(@RequestHeader("X-User-Id") String userId,
                                                     @PathVariable("id") Long orderId,
                                                     @Valid @RequestBody PayOrderRequest request) {
        return ResponseEntity.accepted().body(orderService.payOrder(userId, orderId, request));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@RequestHeader("X-User-Id") String userId,
                                                     @PathVariable("id") Long orderId) {
        return ResponseEntity.ok(orderService.cancelOrderByUser(userId, orderId));
    }

    @PostMapping("/{id}/return")
    public ResponseEntity<OrderResponse> returnOrder(@RequestHeader("X-User-Id") String userId,
                                                     @PathVariable("id") Long orderId) {
        return ResponseEntity.accepted().body(orderService.initiateReturn(userId, orderId));
    }

    @PostMapping("/{id}/finish")
    public ResponseEntity<OrderResponse> finishOrder(@RequestHeader("X-User-Id") String userId,
                                                     @PathVariable("id") Long orderId) {
        return ResponseEntity.ok(orderService.finishOrder(userId, orderId));
    }

    @PostMapping("/{id}/retry-refund")
    public ResponseEntity<OrderResponse> retryRefund(@RequestHeader("X-User-Id") String userId,
                                                     @PathVariable("id") Long orderId) {
        return ResponseEntity.accepted().body(orderService.retryRefund(userId, orderId));
    }

    private void requireMerchant(String roles) {
        if (roles == null || List.of(roles.split(",")).stream().map(String::trim).noneMatch("MERCHANT"::equals)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Merchant role required");
        }
    }

    record UpdateOrderStatusRequest(OrderStatus status) {}
}
