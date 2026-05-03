package com.example.order.service.impl;

import com.example.order.client.CartClient;
import com.example.order.client.PaymentClient;
import com.example.order.client.ProductClient;
import com.example.order.client.UserClient;
import com.example.order.dto.OrderResponse;
import com.example.order.dto.PayOrderRequest;
import com.example.order.dto.PayOrderResponse;
import com.example.order.dto.PlaceOrderRequest;
import com.example.order.dto.StatusHistoryEntry;
import com.example.order.entity.Order;
import com.example.order.entity.OrderItem;
import com.example.order.entity.OrderStatus;
import com.example.order.entity.OrderStatusHistory;
import com.example.lib.exception.InsufficientStockException;
import com.example.order.publisher.OrderEventPublisher;
import com.example.order.repository.OrderRepository;
import com.example.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final CartClient cartClient;
    private final UserClient userClient;
    private final PaymentClient paymentClient;
    private final OrderEventPublisher publisher;

    @Override
    @Transactional
    public OrderResponse placeOrder(String userId, PlaceOrderRequest request) {
        log.debug("placeOrder userId={}", userId);

        var cart = cartClient.getCart(userId);

        if (cart.items() == null || cart.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty");
        }

        BigDecimal total = cart.items().stream()
                .map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var order = Order.builder()
                .userId(userId)
                .status(OrderStatus.PENDING)
                .totalPrice(total)
                .createdAt(LocalDateTime.now())
                .build();

        cart.items().forEach(i -> order.getItems().add(
                OrderItem.builder()
                        .productId(i.productId())
                        .quantity(i.quantity())
                        .unitPrice(i.unitPrice())
                        .build()
        ));

        transitionStatus(order, OrderStatus.PENDING, "Order placed");

        var saved = orderRepository.save(order);
        cartClient.clearCart(userId);
        publisher.publishNotificationOrderCreated(saved.getId(), userId);

        log.info("Order {} placed (PENDING) for userId={} total={} items={}, cart cleared",
                saved.getId(), userId, total, cart.items().size());
        return OrderResponse.from(saved);
    }

    @Override
    @Transactional
    public PayOrderResponse payOrder(String userId, Long orderId, PayOrderRequest request) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!order.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order not owned by user");
        }
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PAYMENT_FAILED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Order must be PENDING or PAYMENT_FAILED to pay, current=" + order.getStatus());
        }

        List<Long> ids = order.getItems().stream().map(OrderItem::getProductId).toList();
        Map<Long, ProductClient.ProductResponse> products = productClient.getProducts(ids).content().stream()
                .collect(Collectors.toMap(ProductClient.ProductResponse::id, Function.identity()));

        for (OrderItem item : order.getItems()) {
            var product = products.get(item.getProductId());
            if (product == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Product not found: " + item.getProductId());
            }
            if (item.getQuantity() > product.stock()) {
                throw new InsufficientStockException(item.getProductId(), item.getQuantity(), product.stock());
            }
        }

        order.getItems().forEach(item -> productClient.deductStock(
                item.getProductId(), new ProductClient.DeductStockRequest(item.getQuantity())));

        var user = userClient.getById(userId);
        if (user.phone() == null || user.addressLine() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "User address incomplete — update via PATCH /api/users/me/address");
        }

        var buyer = new PaymentClient.BuyerInfo(
                user.id(), user.firstName(), user.lastName(),
                user.email(), user.phone(), user.identityNumber(),
                user.addressLine(), user.city(), user.country(), user.zipCode(), null);

        List<PaymentClient.BasketItem> basketItems = order.getItems().stream().map(item -> {
            var product = products.get(item.getProductId());
            return new PaymentClient.BasketItem(
                    String.valueOf(item.getProductId()),
                    product.name(),
                    product.category(),
                    item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }).toList();

        String cardUserKey = resolveCardUserKey(user, request);

        var payReq = new PaymentClient.InitiateRequest(
                order.getId(), request.method().toUpperCase(),
                order.getTotalPrice(), buyer, basketItems,
                cardUserKey, request.forceThreeDS(), request.paymentWithNewCardEnabled());

        var payResp = paymentClient.initiate(payReq);
        log.info("Payment initiated orderId={} method={} token={}", order.getId(), request.method(), payResp.token());
        return new PayOrderResponse(OrderResponse.from(order), payResp.paymentPageUrl(), payResp.token());
    }

    private String resolveCardUserKey(UserClient.UserResponse user, PayOrderRequest request) {
        if (Boolean.TRUE.equals(request.paymentWithNewCardEnabled())) {
            return null;
        }
        Long requestedCardId = request.savedCardId() != null ? request.savedCardId() : user.defaultCardId();
        if (requestedCardId == null || user.cards() == null) {
            return null;
        }
        return user.cards().stream()
                .filter(card -> requestedCardId.equals(card.id()))
                .map(UserClient.CardResponse::iyzipayCardUserKey)
                .filter(key -> key != null && !key.isBlank())
                .findFirst()
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByUser(String userId, LocalDate from, LocalDate to, int page, int size) {
        return orderRepository.findByUserIdAndCreatedAtRange(
                        userId,
                        startOfDay(from),
                        startOfNextDay(to),
                        ordersPageable(page, size)
                )
                .map(OrderResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByUser(String userId, int page, int size) {
        return orderRepository.findByUserId(userId, ordersPageable(page, size))
                .map(OrderResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrders(LocalDate from, LocalDate to, int page, int size) {
        return orderRepository.findByCreatedAtRange(
                        startOfDay(from),
                        startOfNextDay(to),
                        ordersPageable(page, size)
                )
                .map(OrderResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrders(int page, int size) {
        return orderRepository.findAll(ordersPageable(page, size)).map(OrderResponse::from);
    }

    private PageRequest ordersPageable(int page, int size) {
        return PageRequest.of(
                Math.max(page, 0),
                Math.max(size, 1),
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))
        );
    }

    private LocalDateTime startOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private LocalDateTime startOfNextDay(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(String userId, Long orderId) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!order.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order not owned by user");
        }
        return OrderResponse.from(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .map(OrderResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatusHistoryEntry> getOrderHistory(String userId, Long orderId) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!order.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order not owned by user");
        }
        return order.getStatusHistory().stream()
                .sorted(Comparator.comparing(
                        OrderStatusHistory::getChangedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .map(StatusHistoryEntry::from)
                .toList();
    }

    @Override
    @Transactional
    public void updateStatus(Long orderId, OrderStatus status) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        String note = null;
        if (status == OrderStatus.SHIPPED || status == OrderStatus.DELIVERED) {
            try {
                var user = userClient.getById(order.getUserId());
                String fullName = user.firstName() + " " + user.lastName();
                note = status == OrderStatus.SHIPPED
                        ? "Order shipped to " + fullName
                        : "Delivered to " + fullName;
            } catch (Exception e) {
                log.warn("Could not fetch user for status note orderId={}: {}", orderId, e.getMessage());
            }
        }

        transitionStatus(order, status, note);
        orderRepository.save(order);

        if (status == OrderStatus.SHIPPED) {
            publisher.publishNotificationOrderShipped(order.getId(), order.getUserId());
        } else if (status == OrderStatus.DELIVERED) {
            publisher.publishNotificationOrderDelivered(order.getId(), order.getUserId());
        }
    }

    @Override
    @Transactional
    public OrderResponse updateStatusForAdmin(Long orderId, OrderStatus status) {
        updateStatus(orderId, status);
        return getOrderById(orderId);
    }

    @Override
    @Transactional
    public void markPaid(Long orderId) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        if (order.getStatus() == OrderStatus.PAID) {
            log.info("Order {} already PAID, skipping duplicate payment accepted event", orderId);
            return;
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot mark cancelled order as paid: " + orderId);
        }
        transitionStatus(order, OrderStatus.PAID, "Payment accepted");
        orderRepository.save(order);
        publisher.publishNotificationPaymentAccepted(order.getId(), order.getUserId());
        log.info("Order {} marked PAID, cart cleared for userId={}", orderId, order.getUserId());
    }

    @Override
    @Transactional
    public void markPaymentFailed(Long orderId, String reason) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        if (order.getStatus() == OrderStatus.PAYMENT_FAILED) {
            log.info("Order {} already PAYMENT_FAILED, skipping duplicate event", orderId);
            return;
        }
        if (order.getStatus() == OrderStatus.PAID) {
            log.warn("Payment rejected event for already-PAID order {}, ignoring", orderId);
            return;
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            log.warn("Payment rejected event for already-CANCELLED order {}, ignoring", orderId);
            return;
        }
        transitionStatus(order, OrderStatus.PAYMENT_FAILED, reason != null ? reason : "Payment rejected");
        orderRepository.save(order);
        for (OrderItem item : order.getItems()) {
            try {
                productClient.restoreStock(item.getProductId(),
                        new ProductClient.RestoreStockRequest(item.getQuantity()));
            } catch (Exception e) {
                log.error("Failed to restore stock for productId={} orderId={}: {}", item.getProductId(), orderId, e.getMessage());
            }
        }
        publisher.publishNotificationPaymentRejected(order.getId(), order.getUserId(), reason);
        log.info("Order {} marked PAYMENT_FAILED, stock restored for {} items", orderId, order.getItems().size());
    }

    @Override
    @Transactional
    public OrderResponse cancelOrderByUser(String userId, Long orderId) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!order.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order not owned by user");
        }
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PAYMENT_FAILED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only PENDING or PAYMENT_FAILED orders can be cancelled, current=" + order.getStatus());
        }
        transitionStatus(order, OrderStatus.CANCELLED, "Cancelled by customer");
        orderRepository.save(order);
        log.info("Order {} cancelled by userId={}", orderId, userId);
        return OrderResponse.from(order);
    }

    @Override
    @Transactional
    public OrderResponse initiateReturn(String userId, Long orderId) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!order.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order not owned by user");
        }
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Order must be DELIVERED to initiate return, current=" + order.getStatus());
        }
        transitionStatus(order, OrderStatus.RETURNING, "Return initiated by customer");
        orderRepository.save(order);
        publisher.publishReturnRequested(orderId);
        publisher.publishNotificationReturnInitiated(order.getId(), order.getUserId());
        log.info("Return initiated for orderId={} userId={}", orderId, userId);
        return OrderResponse.from(order);
    }

    @Override
    @Transactional
    public OrderResponse finishOrder(String userId, Long orderId) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!order.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order not owned by user");
        }
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Order must be DELIVERED to finish, current=" + order.getStatus());
        }
        transitionStatus(order, OrderStatus.COMPLETED, "Order completed by customer");
        orderRepository.save(order);
        log.info("Order {} completed by userId={}", orderId, userId);
        return OrderResponse.from(order);
    }

    @Override
    @Transactional
    public OrderResponse retryRefund(String userId, Long orderId) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!order.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order not owned by user");
        }
        if (order.getStatus() != OrderStatus.RETURN_FAILED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Order must be in RETURN_FAILED state to retry refund, current=" + order.getStatus());
        }
        transitionStatus(order, OrderStatus.REFUNDING, "Refund retry requested");
        orderRepository.save(order);
        publisher.publishRefundInitiate(orderId);
        log.info("Refund retry initiated for orderId={} userId={}", orderId, userId);
        return OrderResponse.from(order);
    }

    @Override
    @Transactional
    public void markReturnShipped(Long orderId) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        if (order.getStatus() == OrderStatus.RETURN_SHIPPED) {
            log.info("Order {} already RETURN_SHIPPED, skipping", orderId);
            return;
        }
        transitionStatus(order, OrderStatus.RETURN_SHIPPED, "Return package picked up");
        orderRepository.save(order);
        publisher.publishNotificationReturnShipped(order.getId(), order.getUserId());
        log.info("Order {} marked RETURN_SHIPPED", orderId);
    }

    @Override
    @Transactional
    public void markRefunding(Long orderId) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        if (order.getStatus() == OrderStatus.REFUNDING) {
            log.info("Order {} already REFUNDING, skipping", orderId);
            return;
        }
        transitionStatus(order, OrderStatus.REFUNDING, "Return package received at warehouse");
        orderRepository.save(order);
        publisher.publishRefundInitiate(orderId);
        publisher.publishNotificationRefunding(order.getId(), order.getUserId());
        log.info("Order {} marked REFUNDING, refund.initiate published", orderId);
    }

    @Override
    @Transactional
    public void markReturned(Long orderId) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        if (order.getStatus() == OrderStatus.RETURNED) {
            log.info("Order {} already RETURNED, skipping", orderId);
            return;
        }
        transitionStatus(order, OrderStatus.RETURNED, "Refund completed");
        orderRepository.save(order);
        for (OrderItem item : order.getItems()) {
            try {
                productClient.restoreStock(item.getProductId(),
                        new ProductClient.RestoreStockRequest(item.getQuantity()));
            } catch (Exception e) {
                log.error("Failed to restore stock for productId={} orderId={}: {}",
                        item.getProductId(), orderId, e.getMessage());
            }
        }
        publisher.publishNotificationReturned(order.getId(), order.getUserId());
        log.info("Order {} marked RETURNED, stock restored for {} items", orderId, order.getItems().size());
    }

    @Override
    @Transactional
    public void markReturnFailed(Long orderId, String reason) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        if (order.getStatus() == OrderStatus.RETURN_FAILED) {
            log.info("Order {} already RETURN_FAILED, skipping", orderId);
            return;
        }
        transitionStatus(order, OrderStatus.RETURN_FAILED,
                "Refund failed: " + (reason != null ? reason : "unknown reason"));
        orderRepository.save(order);
        publisher.publishNotificationReturnFailed(order.getId(), order.getUserId(), reason);
        log.warn("Order {} marked RETURN_FAILED reason={}", orderId, reason);
    }

    private void transitionStatus(Order order, OrderStatus newStatus, String note) {
        order.setStatus(newStatus);
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(newStatus)
                .note(note)
                .build();
        order.getStatusHistory().add(history);
    }
}
