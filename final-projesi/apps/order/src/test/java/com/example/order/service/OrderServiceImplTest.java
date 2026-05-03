package com.example.order.service;

import com.example.lib.exception.InsufficientStockException;
import com.example.order.client.CartClient;
import com.example.order.client.CartClient.CartItem;
import com.example.order.client.CartClient.CartResponse;
import com.example.order.client.PaymentClient;
import com.example.order.client.ProductClient;
import com.example.order.client.UserClient;
import com.example.order.dto.PayOrderRequest;
import com.example.order.dto.PlaceOrderRequest;
import com.example.order.entity.Order;
import com.example.order.entity.OrderItem;
import com.example.order.entity.OrderStatus;
import com.example.order.publisher.OrderEventPublisher;
import com.example.order.repository.OrderRepository;
import com.example.order.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl")
class OrderServiceImplTest {

    private static final String USER = "user-1";

    @Mock private OrderRepository orderRepository;
    @Mock private ProductClient productClient;
    @Mock private CartClient cartClient;
    @Mock private UserClient userClient;
    @Mock private PaymentClient paymentClient;
    @Mock private OrderEventPublisher publisher;

    @InjectMocks
    private OrderServiceImpl orderService;

    // ─────────────────────────────── helpers ───────────────────────────────

    private static Order order(Long id, String userId, OrderStatus status, OrderItem... items) {
        var o = Order.builder()
                .id(id)
                .userId(userId)
                .status(status)
                .totalPrice(new BigDecimal("100.00"))
                .createdAt(LocalDateTime.now())
                .build();
        for (OrderItem i : items) o.getItems().add(i);
        return o;
    }

    private static OrderItem item(Long productId, int qty, String unitPrice) {
        return OrderItem.builder()
                .productId(productId)
                .quantity(qty)
                .unitPrice(new BigDecimal(unitPrice))
                .build();
    }

    private static CartItem cartItem(Long productId, int qty, String price) {
        return new CartItem(productId, new BigDecimal(price), qty);
    }

    private static ProductClient.ProductResponse product(Long id, String name, String price, int stock) {
        return new ProductClient.ProductResponse(id, name, "cat", new BigDecimal(price), stock);
    }

    private static UserClient.UserResponse fullUser() {
        return new UserClient.UserResponse(
                USER, "uname", "u@e.com", "First", "Last",
                "555", "11111111111", "addr", "city", "TR", "06000",
                null, List.of(), LocalDateTime.now());
    }

    private static UserClient.UserResponse incompleteUser() {
        return new UserClient.UserResponse(
                USER, "uname", "u@e.com", "First", "Last",
                null, null, null, null, null, null,
                null, null, LocalDateTime.now());
    }

    private static PayOrderRequest payRequest() {
        return new PayOrderRequest("IYZICO", null, null, null);
    }

    // ─────────────────────────────── placeOrder ───────────────────────────

    @Nested
    @DisplayName("placeOrder")
    class PlaceOrder {

        @Test
        @DisplayName("empty cart → 400, no save")
        void emptyCart() {
            when(cartClient.getCart(USER)).thenReturn(new CartResponse(USER, List.of(), BigDecimal.ZERO));

            assertThatThrownBy(() -> orderService.placeOrder(USER, new PlaceOrderRequest()))
                    .isInstanceOf(ResponseStatusException.class)
                    .matches(e -> ((ResponseStatusException) e).getStatusCode().value() == 400);

            verify(orderRepository, never()).save(any());
            verify(cartClient, never()).clearCart(any());
        }

        @Test
        @DisplayName("null items list → 400")
        void nullItems() {
            when(cartClient.getCart(USER)).thenReturn(new CartResponse(USER, null, BigDecimal.ZERO));

            assertThatThrownBy(() -> orderService.placeOrder(USER, new PlaceOrderRequest()))
                    .isInstanceOf(ResponseStatusException.class)
                    .matches(e -> ((ResponseStatusException) e).getStatusCode().value() == 400);
        }

        @Test
        @DisplayName("happy path → saves PENDING with computed total, clears cart, single history entry")
        void happyPath() {
            var items = List.of(
                    cartItem(1L, 2, "30.00"),
                    cartItem(2L, 1, "40.00"));
            when(cartClient.getCart(USER)).thenReturn(new CartResponse(USER, items, BigDecimal.ZERO));

            var captor = ArgumentCaptor.forClass(Order.class);
            when(orderRepository.save(captor.capture())).thenAnswer(inv -> {
                Order o = inv.getArgument(0);
                o.setId(99L);
                return o;
            });

            var resp = orderService.placeOrder(USER, new PlaceOrderRequest());

            Order saved = captor.getValue();
            assertThat(saved.getUserId()).isEqualTo(USER);
            assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(saved.getTotalPrice()).isEqualByComparingTo("100.00");
            assertThat(saved.getItems()).hasSize(2);
            assertThat(saved.getStatusHistory()).hasSize(1);
            assertThat(saved.getStatusHistory().get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(saved.getStatusHistory().get(0).getNote()).isEqualTo("Order placed");

            verify(cartClient).clearCart(USER);
            assertThat(resp.id()).isEqualTo(99L);
            assertThat(resp.status()).isEqualTo(OrderStatus.PENDING);
        }

        @Test
        @DisplayName("totalPrice uses cart unitPrice × quantity, BigDecimal precision preserved")
        void precision() {
            var items = List.of(
                    cartItem(1L, 3, "0.10"),
                    cartItem(2L, 7, "0.99"));
            when(cartClient.getCart(USER)).thenReturn(new CartResponse(USER, items, BigDecimal.ZERO));

            var captor = ArgumentCaptor.forClass(Order.class);
            when(orderRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            orderService.placeOrder(USER, new PlaceOrderRequest());

            assertThat(captor.getValue().getTotalPrice()).isEqualByComparingTo("7.23");
        }
    }

    // ─────────────────────────────── payOrder ─────────────────────────────

    @Nested
    @DisplayName("payOrder")
    class PayOrder {

        @Test
        @DisplayName("order not found → 404")
        void notFound() {
            when(orderRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.payOrder(USER, 99L, payRequest()))
                    .isInstanceOf(ResponseStatusException.class)
                    .matches(e -> ((ResponseStatusException) e).getStatusCode().value() == 404);
            verifyNoInteractions(productClient, userClient, paymentClient);
        }

        @Test
        @DisplayName("wrong owner → 403")
        void wrongOwner() {
            var o = order(1L, "other", OrderStatus.PENDING, item(1L, 1, "50.00"));
            when(orderRepository.findById(1L)).thenReturn(Optional.of(o));

            assertThatThrownBy(() -> orderService.payOrder(USER, 1L, payRequest()))
                    .isInstanceOf(ResponseStatusException.class)
                    .matches(e -> ((ResponseStatusException) e).getStatusCode().value() == 403);
        }

        @Test
        @DisplayName("status PAID → 409")
        void alreadyPaid() {
            var o = order(1L, USER, OrderStatus.PAID, item(1L, 1, "50.00"));
            when(orderRepository.findById(1L)).thenReturn(Optional.of(o));

            assertThatThrownBy(() -> orderService.payOrder(USER, 1L, payRequest()))
                    .isInstanceOf(ResponseStatusException.class)
                    .matches(e -> ((ResponseStatusException) e).getStatusCode().value() == 409);
        }

        @Test
        @DisplayName("status PAYMENT_FAILED → allowed (retry)")
        void retryAfterPaymentFailed() {
            var o = order(1L, USER, OrderStatus.PAYMENT_FAILED, item(1L, 2, "50.00"));
            when(orderRepository.findById(1L)).thenReturn(Optional.of(o));
            when(productClient.getProducts(anyList()))
                    .thenReturn(new ProductClient.ProductsPage(List.of(product(1L, "P1", "50.00", 10))));
            when(userClient.getById(USER)).thenReturn(fullUser());
            when(paymentClient.initiate(any()))
                    .thenReturn(new PaymentClient.InitiateResponse(1L, "IYZICO", "tok", "https://p"));

            var result = orderService.payOrder(USER, 1L, payRequest());

            assertThat(result.token()).isEqualTo("tok");
            verify(productClient).deductStock(eq(1L), any());
        }

        @Test
        @DisplayName("missing product in batch → 400, no stock deduction")
        void missingProduct() {
            var o = order(1L, USER, OrderStatus.PENDING, item(1L, 1, "50.00"));
            when(orderRepository.findById(1L)).thenReturn(Optional.of(o));
            when(productClient.getProducts(anyList()))
                    .thenReturn(new ProductClient.ProductsPage(List.of()));

            assertThatThrownBy(() -> orderService.payOrder(USER, 1L, payRequest()))
                    .isInstanceOf(ResponseStatusException.class)
                    .matches(e -> ((ResponseStatusException) e).getStatusCode().value() == 400);

            verify(productClient, never()).deductStock(any(), any());
            verifyNoInteractions(paymentClient);
        }

        @Test
        @DisplayName("insufficient stock → InsufficientStockException, no deduction")
        void insufficientStock() {
            var o = order(1L, USER, OrderStatus.PENDING, item(1L, 5, "50.00"));
            when(orderRepository.findById(1L)).thenReturn(Optional.of(o));
            when(productClient.getProducts(anyList()))
                    .thenReturn(new ProductClient.ProductsPage(List.of(product(1L, "P1", "50.00", 1))));

            assertThatThrownBy(() -> orderService.payOrder(USER, 1L, payRequest()))
                    .isInstanceOf(InsufficientStockException.class);

            verify(productClient, never()).deductStock(any(), any());
            verifyNoInteractions(paymentClient);
        }

        @Test
        @DisplayName("incomplete user address (no phone) → 422, no stock deduction occurred yet? actually after stock deduction")
        void incompleteAddress() {
            var o = order(1L, USER, OrderStatus.PENDING, item(1L, 1, "50.00"));
            when(orderRepository.findById(1L)).thenReturn(Optional.of(o));
            when(productClient.getProducts(anyList()))
                    .thenReturn(new ProductClient.ProductsPage(List.of(product(1L, "P1", "50.00", 10))));
            when(userClient.getById(USER)).thenReturn(incompleteUser());

            assertThatThrownBy(() -> orderService.payOrder(USER, 1L, payRequest()))
                    .isInstanceOf(ResponseStatusException.class)
                    .matches(e -> ((ResponseStatusException) e).getStatusCode().value() == 422);

            verifyNoInteractions(paymentClient);
        }

        @Test
        @DisplayName("happy path → deducts stock, builds basket, calls payment, returns token + url")
        void happyPath() {
            var o = order(1L, USER, OrderStatus.PENDING,
                    item(1L, 2, "50.00"),
                    item(2L, 1, "30.00"));
            when(orderRepository.findById(1L)).thenReturn(Optional.of(o));
            when(productClient.getProducts(anyList()))
                    .thenReturn(new ProductClient.ProductsPage(List.of(
                            product(1L, "P1", "50.00", 10),
                            product(2L, "P2", "30.00", 10))));
            when(userClient.getById(USER)).thenReturn(fullUser());

            var captor = ArgumentCaptor.forClass(PaymentClient.InitiateRequest.class);
            when(paymentClient.initiate(captor.capture()))
                    .thenReturn(new PaymentClient.InitiateResponse(1L, "IYZICO", "tok-1", "https://pay/"));

            var result = orderService.payOrder(USER, 1L, payRequest());

            assertThat(result.token()).isEqualTo("tok-1");
            assertThat(result.paymentPageUrl()).isEqualTo("https://pay/");
            assertThat(result.order().id()).isEqualTo(1L);

            PaymentClient.InitiateRequest req = captor.getValue();
            assertThat(req.orderId()).isEqualTo(1L);
            assertThat(req.method()).isEqualTo("IYZICO");
            assertThat(req.items()).hasSize(2);
            assertThat(req.buyer().id()).isEqualTo(USER);

            verify(productClient, times(2)).deductStock(any(), any());
        }

        @Test
        @DisplayName("method lower-case input → upper-cased before payment call")
        void methodUppercased() {
            var o = order(1L, USER, OrderStatus.PENDING, item(1L, 1, "50.00"));
            when(orderRepository.findById(1L)).thenReturn(Optional.of(o));
            when(productClient.getProducts(anyList()))
                    .thenReturn(new ProductClient.ProductsPage(List.of(product(1L, "P1", "50.00", 10))));
            when(userClient.getById(USER)).thenReturn(fullUser());

            var captor = ArgumentCaptor.forClass(PaymentClient.InitiateRequest.class);
            when(paymentClient.initiate(captor.capture()))
                    .thenReturn(new PaymentClient.InitiateResponse(1L, "IYZICO", "t", "u"));

            orderService.payOrder(USER, 1L,
                    new PayOrderRequest("iyzico", null, null, null));

            assertThat(captor.getValue().method()).isEqualTo("IYZICO");
        }
    }

    // ─────────────────────────────── markPaid ─────────────────────────────

    @Nested
    @DisplayName("markPaid")
    class MarkPaid {

        @Test
        @DisplayName("PENDING → PAID, publishes notification")
        void pendingToPaid() {
            var o = order(1L, USER, OrderStatus.PENDING, item(1L, 1, "50.00"));
            when(orderRepository.findById(1L)).thenReturn(Optional.of(o));

            orderService.markPaid(1L);

            assertThat(o.getStatus()).isEqualTo(OrderStatus.PAID);
            verify(orderRepository).save(o);
            verify(publisher).publishNotificationPaymentAccepted(1L, USER);
        }

        @Test
        @DisplayName("already PAID → no-op (idempotent), no save, no publish")
        void alreadyPaidIdempotent() {
            var o = order(1L, USER, OrderStatus.PAID, item(1L, 1, "50.00"));
            when(orderRepository.findById(1L)).thenReturn(Optional.of(o));

            orderService.markPaid(1L);

            verify(orderRepository, never()).save(any());
            verifyNoInteractions(publisher);
        }

        @Test
        @DisplayName("CANCELLED → 409, no state change")
        void cancelledRejects() {
            var o = order(1L, USER, OrderStatus.CANCELLED, item(1L, 1, "50.00"));
            when(orderRepository.findById(1L)).thenReturn(Optional.of(o));

            assertThatThrownBy(() -> orderService.markPaid(1L))
                    .isInstanceOf(ResponseStatusException.class)
                    .matches(e -> ((ResponseStatusException) e).getStatusCode().value() == 409);

            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("missing order → RuntimeException")
        void missing() {
            when(orderRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.markPaid(99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("99");
        }
    }

    // ─────────────────────────────── markPaymentFailed ────────────────────

    @Nested
    @DisplayName("markPaymentFailed")
    class MarkPaymentFailed {

        @Test
        @DisplayName("PENDING → PAYMENT_FAILED, restores stock per item, publishes")
        void pendingToFailed() {
            var o = order(1L, USER, OrderStatus.PENDING,
                    item(1L, 2, "50.00"),
                    item(2L, 3, "10.00"));
            when(orderRepository.findById(1L)).thenReturn(Optional.of(o));

            orderService.markPaymentFailed(1L, "Declined");

            assertThat(o.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
            verify(productClient).restoreStock(eq(1L), any());
            verify(productClient).restoreStock(eq(2L), any());
            verify(publisher).publishNotificationPaymentRejected(1L, USER, "Declined");
        }

        @Test
        @DisplayName("null reason → uses default reason, still restores stock")
        void nullReason() {
            var o = order(1L, USER, OrderStatus.PENDING, item(1L, 1, "50.00"));
            when(orderRepository.findById(1L)).thenReturn(Optional.of(o));

            orderService.markPaymentFailed(1L, null);

            assertThat(o.getStatusHistory().getLast().getNote()).isEqualTo("Payment rejected");
        }

        @Test
        @DisplayName("already PAYMENT_FAILED → no-op")
        void idempotent() {
            var o = order(1L, USER, OrderStatus.PAYMENT_FAILED, item(1L, 1, "50.00"));
            when(orderRepository.findById(1L)).thenReturn(Optional.of(o));

            orderService.markPaymentFailed(1L, "x");

            verify(orderRepository, never()).save(any());
            verify(productClient, never()).restoreStock(any(), any());
        }

        @Test
        @DisplayName("PAID → ignored (don't undo paid)")
        void paidIgnored() {
            var o = order(1L, USER, OrderStatus.PAID, item(1L, 1, "50.00"));
            when(orderRepository.findById(1L)).thenReturn(Optional.of(o));

            orderService.markPaymentFailed(1L, "late event");

            verify(productClient, never()).restoreStock(any(), any());
            assertThat(o.getStatus()).isEqualTo(OrderStatus.PAID);
        }

        @Test
        @DisplayName("restoreStock failure does not abort other items")
        void restorePartialFailure() {
            var o = order(1L, USER, OrderStatus.PENDING,
                    item(1L, 1, "50.00"),
                    item(2L, 1, "10.00"));
            when(orderRepository.findById(1L)).thenReturn(Optional.of(o));
            org.mockito.Mockito.doThrow(new RuntimeException("down"))
                    .when(productClient).restoreStock(eq(1L), any());

            orderService.markPaymentFailed(1L, "x");

            verify(productClient).restoreStock(eq(2L), any());
            assertThat(o.getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        }
    }

    // ─────────────────────────────── cancelOrderByUser ────────────────────

    @Nested
    @DisplayName("cancelOrderByUser")
    class Cancel {

        @Test
        @DisplayName("PENDING → CANCELLED")
        void pending() {
            var o = order(1L, USER, OrderStatus.PENDING, item(1L, 1, "50.00"));
            when(orderRepository.findById(1L)).thenReturn(Optional.of(o));

            orderService.cancelOrderByUser(USER, 1L);

            assertThat(o.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("PAYMENT_FAILED → CANCELLED")
        void paymentFailed() {
            var o = order(1L, USER, OrderStatus.PAYMENT_FAILED, item(1L, 1, "50.00"));
            when(orderRepository.findById(1L)).thenReturn(Optional.of(o));

            orderService.cancelOrderByUser(USER, 1L);

            assertThat(o.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("PAID → 409")
        void paidRejects() {
            var o = order(1L, USER, OrderStatus.PAID, item(1L, 1, "50.00"));
            when(orderRepository.findById(1L)).thenReturn(Optional.of(o));

            assertThatThrownBy(() -> orderService.cancelOrderByUser(USER, 1L))
                    .isInstanceOf(ResponseStatusException.class)
                    .matches(e -> ((ResponseStatusException) e).getStatusCode().value() == 409);
        }

        @Test
        @DisplayName("DELIVERED → 409")
        void deliveredRejects() {
            var o = order(1L, USER, OrderStatus.DELIVERED, item(1L, 1, "50.00"));
            when(orderRepository.findById(1L)).thenReturn(Optional.of(o));

            assertThatThrownBy(() -> orderService.cancelOrderByUser(USER, 1L))
                    .isInstanceOf(ResponseStatusException.class)
                    .matches(e -> ((ResponseStatusException) e).getStatusCode().value() == 409);
        }

        @Test
        @DisplayName("wrong user → 403")
        void wrongOwner() {
            var o = order(1L, "other", OrderStatus.PENDING, item(1L, 1, "50.00"));
            when(orderRepository.findById(1L)).thenReturn(Optional.of(o));

            assertThatThrownBy(() -> orderService.cancelOrderByUser(USER, 1L))
                    .isInstanceOf(ResponseStatusException.class)
                    .matches(e -> ((ResponseStatusException) e).getStatusCode().value() == 403);
        }

        @Test
        @DisplayName("not found → 404")
        void notFound() {
            when(orderRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.cancelOrderByUser(USER, 99L))
                    .isInstanceOf(ResponseStatusException.class)
                    .matches(e -> ((ResponseStatusException) e).getStatusCode().value() == 404);
        }
    }

    // ─────────────────────────────── updateStatus ─────────────────────────

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatus {

        @Test
        @DisplayName("SHIPPED → publishes order shipped notification with user name")
        void shippedPublishes() {
            var o = order(1L, USER, OrderStatus.PAID, item(1L, 1, "50.00"));
            when(orderRepository.findById(1L)).thenReturn(Optional.of(o));
            when(userClient.getById(USER)).thenReturn(fullUser());

            orderService.updateStatus(1L, OrderStatus.SHIPPED);

            assertThat(o.getStatus()).isEqualTo(OrderStatus.SHIPPED);
            assertThat(o.getStatusHistory().getLast().getNote()).contains("First Last");
            verify(publisher).publishNotificationOrderShipped(1L, USER);
        }

        @Test
        @DisplayName("DELIVERED → publishes delivered notification")
        void delivered() {
            var o = order(1L, USER, OrderStatus.SHIPPED, item(1L, 1, "50.00"));
            when(orderRepository.findById(1L)).thenReturn(Optional.of(o));
            when(userClient.getById(USER)).thenReturn(fullUser());

            orderService.updateStatus(1L, OrderStatus.DELIVERED);

            verify(publisher).publishNotificationOrderDelivered(1L, USER);
        }

        @Test
        @DisplayName("user fetch fails → status still changes, no publish blow-up")
        void userFetchFailsButStatusChanges() {
            var o = order(1L, USER, OrderStatus.PAID, item(1L, 1, "50.00"));
            when(orderRepository.findById(1L)).thenReturn(Optional.of(o));
            when(userClient.getById(USER)).thenThrow(new RuntimeException("user down"));

            orderService.updateStatus(1L, OrderStatus.SHIPPED);

            assertThat(o.getStatus()).isEqualTo(OrderStatus.SHIPPED);
            verify(publisher).publishNotificationOrderShipped(1L, USER);
        }

        @Test
        @DisplayName("non-shipped/delivered status → no notification")
        void noNotification() {
            var o = order(1L, USER, OrderStatus.PENDING, item(1L, 1, "50.00"));
            when(orderRepository.findById(1L)).thenReturn(Optional.of(o));

            orderService.updateStatus(1L, OrderStatus.PAID);

            verifyNoInteractions(publisher);
        }
    }

    // ─────────────────────────────── return flow ──────────────────────────

    @Nested
    @DisplayName("initiateReturn")
    class InitiateReturn {

        @Test
        @DisplayName("DELIVERED → RETURNING, publishes return.requested")
        void delivered() {
            var o = order(1L, USER, OrderStatus.DELIVERED, item(1L, 1, "50.00"));
            when(orderRepository.findById(1L)).thenReturn(Optional.of(o));

            orderService.initiateReturn(USER, 1L);

            assertThat(o.getStatus()).isEqualTo(OrderStatus.RETURNING);
            verify(publisher).publishReturnRequested(1L);
            verify(publisher).publishNotificationReturnInitiated(1L, USER);
        }

        @Test
        @DisplayName("PAID → 409")
        void notDelivered() {
            var o = order(1L, USER, OrderStatus.PAID, item(1L, 1, "50.00"));
            when(orderRepository.findById(1L)).thenReturn(Optional.of(o));

            assertThatThrownBy(() -> orderService.initiateReturn(USER, 1L))
                    .isInstanceOf(ResponseStatusException.class)
                    .matches(e -> ((ResponseStatusException) e).getStatusCode().value() == 409);
        }
    }

    @Nested
    @DisplayName("finishOrder")
    class Finish {

        @Test
        @DisplayName("DELIVERED → COMPLETED")
        void completes() {
            var o = order(1L, USER, OrderStatus.DELIVERED, item(1L, 1, "50.00"));
            when(orderRepository.findById(1L)).thenReturn(Optional.of(o));

            orderService.finishOrder(USER, 1L);

            assertThat(o.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        }

        @Test
        @DisplayName("PAID → 409")
        void rejectsNonDelivered() {
            var o = order(1L, USER, OrderStatus.PAID, item(1L, 1, "50.00"));
            when(orderRepository.findById(1L)).thenReturn(Optional.of(o));

            assertThatThrownBy(() -> orderService.finishOrder(USER, 1L))
                    .isInstanceOf(ResponseStatusException.class)
                    .matches(e -> ((ResponseStatusException) e).getStatusCode().value() == 409);
        }
    }

    @Nested
    @DisplayName("retryRefund")
    class RetryRefund {

        @Test
        @DisplayName("RETURN_FAILED → REFUNDING, publishes refund.initiate")
        void retries() {
            var o = order(1L, USER, OrderStatus.RETURN_FAILED, item(1L, 1, "50.00"));
            when(orderRepository.findById(1L)).thenReturn(Optional.of(o));

            orderService.retryRefund(USER, 1L);

            assertThat(o.getStatus()).isEqualTo(OrderStatus.REFUNDING);
            verify(publisher).publishRefundInitiate(1L);
        }

        @Test
        @DisplayName("PAID → 409")
        void rejectsWrongStatus() {
            var o = order(1L, USER, OrderStatus.PAID, item(1L, 1, "50.00"));
            when(orderRepository.findById(1L)).thenReturn(Optional.of(o));

            assertThatThrownBy(() -> orderService.retryRefund(USER, 1L))
                    .isInstanceOf(ResponseStatusException.class)
                    .matches(e -> ((ResponseStatusException) e).getStatusCode().value() == 409);
        }
    }

    @Nested
    @DisplayName("markReturned")
    class MarkReturned {

        @Test
        @DisplayName("REFUNDING → RETURNED, restores stock per item, publishes")
        void returned() {
            var o = order(1L, USER, OrderStatus.REFUNDING,
                    item(1L, 2, "50.00"),
                    item(2L, 3, "10.00"));
            when(orderRepository.findById(1L)).thenReturn(Optional.of(o));

            orderService.markReturned(1L);

            assertThat(o.getStatus()).isEqualTo(OrderStatus.RETURNED);
            verify(productClient).restoreStock(eq(1L), any());
            verify(productClient).restoreStock(eq(2L), any());
            verify(publisher).publishNotificationReturned(1L, USER);
        }

        @Test
        @DisplayName("already RETURNED → no-op")
        void idempotent() {
            var o = order(1L, USER, OrderStatus.RETURNED, item(1L, 1, "50.00"));
            when(orderRepository.findById(1L)).thenReturn(Optional.of(o));

            orderService.markReturned(1L);

            verify(productClient, never()).restoreStock(any(), any());
        }
    }

    @Nested
    @DisplayName("markReturnFailed")
    class MarkReturnFailed {

        @Test
        @DisplayName("→ RETURN_FAILED with reason, publishes")
        void fail() {
            var o = order(1L, USER, OrderStatus.REFUNDING, item(1L, 1, "50.00"));
            when(orderRepository.findById(1L)).thenReturn(Optional.of(o));

            orderService.markReturnFailed(1L, "bank refused");

            assertThat(o.getStatus()).isEqualTo(OrderStatus.RETURN_FAILED);
            assertThat(o.getStatusHistory().getLast().getNote()).contains("bank refused");
            verify(publisher).publishNotificationReturnFailed(1L, USER, "bank refused");
        }

        @Test
        @DisplayName("null reason → \"unknown reason\" suffix")
        void nullReason() {
            var o = order(1L, USER, OrderStatus.REFUNDING, item(1L, 1, "50.00"));
            when(orderRepository.findById(1L)).thenReturn(Optional.of(o));

            orderService.markReturnFailed(1L, null);

            assertThat(o.getStatusHistory().getLast().getNote()).contains("unknown reason");
        }
    }

    // ─────────────────────────────── queries ──────────────────────────────

    @Nested
    @DisplayName("getOrderById (user-scoped)")
    class GetOrderByIdScoped {

        @Test
        @DisplayName("not found → 404")
        void notFound() {
            when(orderRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOrderById(USER, 99L))
                    .isInstanceOf(ResponseStatusException.class)
                    .matches(e -> ((ResponseStatusException) e).getStatusCode().value() == 404);
        }

        @Test
        @DisplayName("wrong owner → 403")
        void wrongOwner() {
            var o = order(1L, "other", OrderStatus.PAID, item(1L, 1, "50.00"));
            when(orderRepository.findById(1L)).thenReturn(Optional.of(o));

            assertThatThrownBy(() -> orderService.getOrderById(USER, 1L))
                    .isInstanceOf(ResponseStatusException.class)
                    .matches(e -> ((ResponseStatusException) e).getStatusCode().value() == 403);
        }

        @Test
        @DisplayName("owner match → returns response")
        void ok() {
            var o = order(1L, USER, OrderStatus.PAID, item(1L, 1, "50.00"));
            when(orderRepository.findById(1L)).thenReturn(Optional.of(o));

            var resp = orderService.getOrderById(USER, 1L);

            assertThat(resp.id()).isEqualTo(1L);
            assertThat(resp.userId()).isEqualTo(USER);
        }
    }

    @Nested
    @DisplayName("getOrdersByUser pagination")
    class GetOrdersByUserPagination {

        @Test
        @DisplayName("uses DESC sort by createdAt then id")
        void sortOrder() {
            var o = order(1L, USER, OrderStatus.PAID, item(1L, 1, "50.00"));
            var pageable = PageRequest.of(0, 6,
                    Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
            when(orderRepository.findByUserId(USER, pageable)).thenReturn(new PageImpl<>(List.of(o)));

            var page = orderService.getOrdersByUser(USER, 0, 6);

            assertThat(page.getContent()).hasSize(1);
            verify(orderRepository).findByUserId(USER, pageable);
        }

        @Test
        @DisplayName("negative page clamped to 0, size<1 clamped to 1")
        void clamps() {
            when(orderRepository.findByUserId(eq(USER), any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            orderService.getOrdersByUser(USER, -5, 0);

            var captor = ArgumentCaptor.forClass(PageRequest.class);
            verify(orderRepository).findByUserId(eq(USER), captor.capture());
            assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
            assertThat(captor.getValue().getPageSize()).isEqualTo(1);
        }
    }
}
