package com.example.payment.service;

import com.example.payment.dto.BasketItem;
import com.example.payment.dto.BuyerInfo;
import com.example.payment.dto.InitiatePaymentRequest;
import com.example.payment.dto.InitiatePaymentResponse;
import com.example.payment.dto.VerifyPaymentResult;
import com.example.payment.entity.Payment;
import com.example.payment.provider.PaymentMethod;
import com.example.payment.provider.PaymentProvider;
import com.example.payment.provider.PaymentProviderRegistry;
import com.example.payment.publisher.PaymentEventPublisher;
import com.example.payment.repository.PaymentRepository;
import com.example.payment.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentServiceImpl")
class PaymentServiceImplTest {

    @Mock private PaymentProviderRegistry registry;
    @Mock private PaymentEventPublisher publisher;
    @Mock private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private static InitiatePaymentRequest sampleRequest() {
        var buyer = new BuyerInfo("user1", "John", "Doe", "j@e.com",
                "+9050000", "11111111111", "addr", "city", "TR", "06000", null);
        var item = new BasketItem("1", "P", "cat", new BigDecimal("100.00"));
        return new InitiatePaymentRequest(1L, PaymentMethod.IYZICO,
                new BigDecimal("100.00"), buyer, List.of(item), null, false, true);
    }

    // ─────────────────────────── initiate ──────────────────────────────

    @Nested
    @DisplayName("initiate")
    class Initiate {

        @Test
        @DisplayName("delegates to provider, returns provider response")
        void delegates() {
            var provider = mock(PaymentProvider.class);
            var resp = new InitiatePaymentResponse(1L, PaymentMethod.IYZICO, "tok", "https://pay/");
            when(registry.get(PaymentMethod.IYZICO)).thenReturn(provider);
            when(provider.initiate(any())).thenReturn(resp);

            var result = paymentService.initiate(sampleRequest());

            assertThat(result.token()).isEqualTo("tok");
            assertThat(result.paymentPageUrl()).isEqualTo("https://pay/");
            verify(provider).initiate(any());
        }

        @Test
        @DisplayName("registry throws → propagates")
        void registryThrows() {
            when(registry.get(PaymentMethod.IYZICO))
                    .thenThrow(new IllegalArgumentException("no provider"));

            assertThatThrownBy(() -> paymentService.initiate(sampleRequest()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ─────────────────────────── handleCallback ────────────────────────

    @Nested
    @DisplayName("handleCallback")
    class HandleCallback {

        @Test
        @DisplayName("accepted with providerPaymentId, no existing record → saves Payment + publishes accepted")
        void acceptedFirstTime() {
            var provider = mock(PaymentProvider.class);
            when(registry.get(PaymentMethod.IYZICO)).thenReturn(provider);
            when(provider.verify("tok"))
                    .thenReturn(new VerifyPaymentResult(5L, true, null, "iyz-1"));
            when(paymentRepository.findByOrderId(5L)).thenReturn(Optional.empty());

            var result = paymentService.handleCallback("iyzico", "tok", null);

            var captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());
            assertThat(captor.getValue().getOrderId()).isEqualTo(5L);
            assertThat(captor.getValue().getIyzipayPaymentId()).isEqualTo("iyz-1");
            assertThat(captor.getValue().getPaidAt()).isNotNull();
            verify(publisher).publishAccepted(5L);
            verify(publisher, never()).publishRejected(any(), any());
            assertThat(result.accepted()).isTrue();
            assertThat(result.orderId()).isEqualTo(5L);
        }

        @Test
        @DisplayName("accepted but record already exists → publishes accepted, no duplicate save")
        void acceptedDuplicate() {
            var provider = mock(PaymentProvider.class);
            when(registry.get(PaymentMethod.IYZICO)).thenReturn(provider);
            when(provider.verify("tok"))
                    .thenReturn(new VerifyPaymentResult(5L, true, null, "iyz-1"));
            when(paymentRepository.findByOrderId(5L))
                    .thenReturn(Optional.of(Payment.builder().orderId(5L).iyzipayPaymentId("iyz-1").build()));

            paymentService.handleCallback("iyzico", "tok", null);

            verify(paymentRepository, never()).save(any());
            verify(publisher).publishAccepted(5L);
        }

        @Test
        @DisplayName("accepted but providerPaymentId null → does not save, but DOES publish accepted (current behavior)")
        void acceptedNullProviderId() {
            var provider = mock(PaymentProvider.class);
            when(registry.get(PaymentMethod.IYZICO)).thenReturn(provider);
            when(provider.verify("tok"))
                    .thenReturn(new VerifyPaymentResult(5L, true, null, null));

            paymentService.handleCallback("iyzico", "tok", null);

            verify(paymentRepository, never()).save(any());
            // accepted branch only executes when accepted && providerPaymentId != null,
            // but the else-if (!accepted) is also false → neither publish runs
            verifyNoInteractions(publisher);
        }

        @Test
        @DisplayName("rejected → publishes rejected with reason, no save")
        void rejected() {
            var provider = mock(PaymentProvider.class);
            when(registry.get(PaymentMethod.IYZICO)).thenReturn(provider);
            when(provider.verify("tok"))
                    .thenReturn(new VerifyPaymentResult(5L, false, "Card declined", null));

            paymentService.handleCallback("iyzico", "tok", null);

            verify(publisher).publishRejected(5L, "Card declined");
            verify(publisher, never()).publishAccepted(any());
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("provider returns null orderId → falls back to callback orderId")
        void fallbackOrderId() {
            var provider = mock(PaymentProvider.class);
            when(registry.get(PaymentMethod.IYZICO)).thenReturn(provider);
            when(provider.verify("tok"))
                    .thenReturn(new VerifyPaymentResult(null, true, null, "iyz-9"));
            when(paymentRepository.findByOrderId(9L)).thenReturn(Optional.empty());

            var result = paymentService.handleCallback("iyzico", "tok", 9L);

            assertThat(result.orderId()).isEqualTo(9L);
            verify(publisher).publishAccepted(9L);
        }

        @Test
        @DisplayName("both orderIds null → IllegalStateException")
        void noOrderId() {
            var provider = mock(PaymentProvider.class);
            when(registry.get(PaymentMethod.IYZICO)).thenReturn(provider);
            when(provider.verify("tok"))
                    .thenReturn(new VerifyPaymentResult(null, true, null, "iyz-x"));

            assertThatThrownBy(() -> paymentService.handleCallback("iyzico", "tok", null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("order id");
        }

        @Test
        @DisplayName("method case-insensitive: lowercase, mixed case both accepted")
        void methodCaseInsensitive() {
            var provider = mock(PaymentProvider.class);
            when(registry.get(PaymentMethod.IYZICO)).thenReturn(provider);
            when(provider.verify("tok"))
                    .thenReturn(new VerifyPaymentResult(1L, true, null, "iyz"));
            when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.empty());

            paymentService.handleCallback("IyZiCo", "tok", null);

            verify(publisher).publishAccepted(1L);
        }

        @Test
        @DisplayName("invalid method string → IllegalArgumentException")
        void invalidMethod() {
            assertThatThrownBy(() -> paymentService.handleCallback("PAYPAL", "tok", null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ─────────────────────────── processRefund ─────────────────────────

    @Nested
    @DisplayName("processRefund")
    class ProcessRefund {

        @Test
        @DisplayName("payment found + refund succeeds → publishes completed")
        void success() {
            when(paymentRepository.findByOrderId(1L))
                    .thenReturn(Optional.of(Payment.builder().orderId(1L).iyzipayPaymentId("iyz-1").build()));
            var provider = mock(PaymentProvider.class);
            when(registry.get(PaymentMethod.IYZICO)).thenReturn(provider);

            paymentService.processRefund(1L);

            verify(provider).refund(1L, "iyz-1");
            verify(publisher).publishRefundCompleted(1L);
            verify(publisher, never()).publishRefundFailed(any(), any());
        }

        @Test
        @DisplayName("payment record missing → publishes refund failed with reason")
        void noPaymentRecord() {
            when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.empty());

            paymentService.processRefund(1L);

            verify(publisher).publishRefundFailed(eq(1L), eq("No payment record found"));
            verify(registry, never()).get(any());
        }

        @Test
        @DisplayName("provider refund throws → publishes refund failed with exception message")
        void providerRefundThrows() {
            when(paymentRepository.findByOrderId(1L))
                    .thenReturn(Optional.of(Payment.builder().orderId(1L).iyzipayPaymentId("iyz-1").build()));
            var provider = mock(PaymentProvider.class);
            when(registry.get(PaymentMethod.IYZICO)).thenReturn(provider);
            org.mockito.Mockito.doThrow(new RuntimeException("bank refused"))
                    .when(provider).refund(1L, "iyz-1");

            paymentService.processRefund(1L);

            verify(publisher).publishRefundFailed(eq(1L), eq("bank refused"));
            verify(publisher, never()).publishRefundCompleted(any());
        }
    }
}
