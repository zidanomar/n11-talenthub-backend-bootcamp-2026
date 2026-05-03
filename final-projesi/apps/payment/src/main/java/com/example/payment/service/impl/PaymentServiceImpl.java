package com.example.payment.service.impl;

import com.example.payment.dto.InitiatePaymentRequest;
import com.example.payment.dto.InitiatePaymentResponse;
import com.example.payment.dto.VerifyPaymentResult;
import com.example.payment.entity.Payment;
import com.example.payment.provider.PaymentMethod;
import com.example.payment.provider.PaymentProvider;
import com.example.payment.provider.PaymentProviderRegistry;
import com.example.payment.publisher.PaymentEventPublisher;
import com.example.payment.repository.PaymentRepository;
import com.example.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentProviderRegistry registry;
    private final PaymentEventPublisher publisher;
    private final PaymentRepository paymentRepository;

    @Override
    public InitiatePaymentResponse initiate(InitiatePaymentRequest request) {
        PaymentProvider provider = registry.get(request.method());
        log.info("Initiating payment orderId={} method={}", request.orderId(), request.method());
        return provider.initiate(request);
    }

    @Override
    @Transactional
    public VerifyPaymentResult handleCallback(String method, String token, Long callbackOrderId) {
        PaymentMethod paymentMethod = PaymentMethod.valueOf(method.toUpperCase());
        PaymentProvider provider = registry.get(paymentMethod);

        var result = provider.verify(token);
        Long orderId = result.orderId() != null ? result.orderId() : callbackOrderId;
        if (orderId == null) {
            throw new IllegalStateException("Payment callback did not include an order id");
        }

        log.info("Payment callback orderId={} accepted={} method={} providerPaymentId={}",
                orderId, result.accepted(), method, result.providerPaymentId());

        if (result.accepted() && result.providerPaymentId() != null) {
            paymentRepository.findByOrderId(orderId).ifPresentOrElse(
                    p -> log.debug("Payment record already exists for orderId={}", orderId),
                    () -> paymentRepository.save(Payment.builder()
                            .orderId(orderId)
                            .iyzipayPaymentId(result.providerPaymentId())
                            .paidAt(LocalDateTime.now())
                            .build())
            );
            publishAfterCommit(() -> publisher.publishAccepted(orderId));
        } else if (!result.accepted()) {
            publishAfterCommit(() -> publisher.publishRejected(orderId, result.reason()));
        }
        return new VerifyPaymentResult(orderId, result.accepted(), result.reason(), result.providerPaymentId());
    }

    @Override
    @Transactional
    public void processRefund(Long orderId) {
        var paymentOpt = paymentRepository.findByOrderId(orderId);
        if (paymentOpt.isEmpty()) {
            log.error("No payment record found for orderId={}, cannot refund", orderId);
            publisher.publishRefundFailed(orderId, "No payment record found");
            return;
        }

        String iyzipayPaymentId = paymentOpt.get().getIyzipayPaymentId();
        PaymentProvider provider = registry.get(PaymentMethod.IYZICO);
        try {
            provider.refund(orderId, iyzipayPaymentId);
            publishAfterCommit(() -> publisher.publishRefundCompleted(orderId));
            log.info("Refund successful orderId={} iyzipayPaymentId={}", orderId, iyzipayPaymentId);
        } catch (Exception e) {
            log.error("Refund failed orderId={} iyzipayPaymentId={}: {}", orderId, iyzipayPaymentId, e.getMessage());
            publishAfterCommit(() -> publisher.publishRefundFailed(orderId, e.getMessage()));
        }
    }

    private void publishAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
