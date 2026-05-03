package com.example.payment.consumer;

import com.example.payment.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentEventConsumer")
class PaymentEventConsumerTest {

    @Mock
    private PaymentService paymentService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PaymentEventConsumer consumer;

    @Test
    @DisplayName("refund.initiate with orderId → processRefund(orderId)")
    void refundInitiate() {
        consumer.handleRefundInitiate("{\"orderId\":42}");

        verify(paymentService).processRefund(42L);
    }

    @Test
    @DisplayName("orderId as long → parsed correctly")
    void largeOrderId() {
        consumer.handleRefundInitiate("{\"orderId\":9999999999}");

        verify(paymentService).processRefund(9999999999L);
    }

    @Test
    @DisplayName("malformed JSON → throws (NACK to RabbitMQ)")
    void malformed() {
        assertThatThrownBy(() -> consumer.handleRefundInitiate("not-json"))
                .isInstanceOf(Exception.class);
        verifyNoInteractions(paymentService);
    }

    @Test
    @DisplayName("missing orderId field → NullPointerException")
    void missingOrderId() {
        assertThatThrownBy(() -> consumer.handleRefundInitiate("{}"))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(paymentService);
    }
}
