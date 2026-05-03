package com.example.payment.consumer;

import com.example.payment.config.RabbitMQConfig;
import com.example.payment.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "#{rabbitMQConfig.paymentRefundInitiateQueue}")
    @SneakyThrows
    public void handleRefundInitiate(String message) {
        log.info("refund.initiate received: {}", message);
        Map<?, ?> event = objectMapper.readValue(message, Map.class);
        Long orderId = ((Number) event.get("orderId")).longValue();
        paymentService.processRefund(orderId);
    }
}
