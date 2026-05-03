package com.example.order.consumer;

import com.example.order.entity.OrderStatus;
import com.example.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderEventConsumer")
class OrderEventConsumerTest {

    @Mock
    private OrderService orderService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private OrderEventConsumer consumer;

    // ─────────────────────────── payment.accepted ───────────────────────────

    @Nested
    @DisplayName("handlePaymentAccepted")
    class PaymentAccepted {

        @Test
        @DisplayName("extracts orderId → markPaid")
        void ok() {
            consumer.handlePaymentAccepted("{\"orderId\":42}");

            verify(orderService).markPaid(42L);
        }

        @Test
        @DisplayName("orderId as integer string → parsed correctly")
        void integerJson() {
            consumer.handlePaymentAccepted("{\"orderId\":1}");

            verify(orderService).markPaid(1L);
        }

        @Test
        @DisplayName("malformed JSON → throws (NACK to RabbitMQ)")
        void malformed() {
            assertThatThrownBy(() -> consumer.handlePaymentAccepted("not-json"))
                    .isInstanceOf(Exception.class);
            verifyNoInteractions(orderService);
        }
    }

    // ─────────────────────────── payment.rejected ───────────────────────────

    @Nested
    @DisplayName("handlePaymentRejected")
    class PaymentRejected {

        @Test
        @DisplayName("with reason → markPaymentFailed(orderId, reason)")
        void withReason() {
            consumer.handlePaymentRejected("{\"orderId\":7,\"reason\":\"Declined\"}");

            verify(orderService).markPaymentFailed(7L, "Declined");
        }

        @Test
        @DisplayName("without reason → markPaymentFailed(orderId, null)")
        void withoutReason() {
            consumer.handlePaymentRejected("{\"orderId\":7}");

            verify(orderService).markPaymentFailed(eq(7L), isNull());
        }
    }

    // ─────────────────────────── shipping update ────────────────────────────

    @Nested
    @DisplayName("handleShippingUpdate")
    class ShippingUpdate {

        @Test
        @DisplayName("shipping.processing → SHIPPED")
        void processing() {
            consumer.handleShippingUpdate("{\"orderId\":10}", "shipping.processing");

            verify(orderService).updateStatus(10L, OrderStatus.SHIPPED);
        }

        @Test
        @DisplayName("shipping.delivered → DELIVERED")
        void delivered() {
            consumer.handleShippingUpdate("{\"orderId\":10}", "shipping.delivered");

            verify(orderService).updateStatus(10L, OrderStatus.DELIVERED);
        }

        @Test
        @DisplayName("unknown routing key (e.g. shipping.delivering) → ignored, no updateStatus")
        void unknown() {
            consumer.handleShippingUpdate("{\"orderId\":10}", "shipping.delivering");

            verify(orderService, never()).updateStatus(any(), any());
        }

        @Test
        @DisplayName("empty routing key → ignored")
        void emptyRoutingKey() {
            consumer.handleShippingUpdate("{\"orderId\":10}", "");

            verify(orderService, never()).updateStatus(any(), any());
        }
    }

    // ─────────────────────────── return / refund flow ──────────────────────

    @Nested
    @DisplayName("return + refund handlers")
    class ReturnRefund {

        @Test
        @DisplayName("return.shipped → markReturnShipped")
        void returnShipped() {
            consumer.handleReturnShipped("{\"orderId\":5}");

            verify(orderService).markReturnShipped(5L);
        }

        @Test
        @DisplayName("return.delivered → markRefunding")
        void returnDelivered() {
            consumer.handleReturnDelivered("{\"orderId\":5}");

            verify(orderService).markRefunding(5L);
        }

        @Test
        @DisplayName("refund.completed → markReturned")
        void refundCompleted() {
            consumer.handleRefundCompleted("{\"orderId\":5}");

            verify(orderService).markReturned(5L);
        }

        @Test
        @DisplayName("refund.failed with reason → markReturnFailed")
        void refundFailedWithReason() {
            consumer.handleRefundFailed("{\"orderId\":5,\"reason\":\"bank refused\"}");

            verify(orderService).markReturnFailed(5L, "bank refused");
        }

        @Test
        @DisplayName("refund.failed without reason → null reason")
        void refundFailedNullReason() {
            consumer.handleRefundFailed("{\"orderId\":5}");

            verify(orderService).markReturnFailed(eq(5L), isNull());
        }
    }
}
