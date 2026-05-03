package com.example.order.service;

import com.example.order.dto.OrderResponse;
import com.example.order.dto.PayOrderRequest;
import com.example.order.dto.PayOrderResponse;
import com.example.order.dto.PlaceOrderRequest;
import com.example.order.dto.StatusHistoryEntry;
import com.example.order.entity.OrderStatus;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

public interface OrderService {
    OrderResponse placeOrder(String userId, PlaceOrderRequest request);
    PayOrderResponse payOrder(String userId, Long orderId, PayOrderRequest request);
    Page<OrderResponse> getOrdersByUser(String userId, LocalDate from, LocalDate to, int page, int size);
    Page<OrderResponse> getOrdersByUser(String userId, int page, int size);
    Page<OrderResponse> getOrders(LocalDate from, LocalDate to, int page, int size);
    Page<OrderResponse> getOrders(int page, int size);
    OrderResponse getOrderById(String userId, Long orderId);
    OrderResponse getOrderById(Long orderId);
    List<StatusHistoryEntry> getOrderHistory(String userId, Long orderId);
    void updateStatus(Long orderId, OrderStatus status);
    OrderResponse updateStatusForAdmin(Long orderId, OrderStatus status);
    void markPaid(Long orderId);
    void markPaymentFailed(Long orderId, String reason);
    OrderResponse cancelOrderByUser(String userId, Long orderId);
    OrderResponse initiateReturn(String userId, Long orderId);
    OrderResponse finishOrder(String userId, Long orderId);
    OrderResponse retryRefund(String userId, Long orderId);
    void markReturnShipped(Long orderId);
    void markRefunding(Long orderId);
    void markReturned(Long orderId);
    void markReturnFailed(Long orderId, String reason);
}
