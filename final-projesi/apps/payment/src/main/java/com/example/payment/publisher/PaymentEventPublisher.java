package com.example.payment.publisher;

import com.example.payment.config.RabbitMQConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQConfig config;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    public void publishAccepted(Long orderId) {
        String payload = objectMapper.writeValueAsString(Map.of("orderId", orderId));
        rabbitTemplate.convertAndSend(config.getExchange(), config.getPaymentAccepted(), payload);
        log.info("Published payment.accepted event orderId={} exchange={} routingKey={}",
                orderId, config.getExchange(), config.getPaymentAccepted());
    }

    @SneakyThrows
    public void publishRejected(Long orderId, String reason) {
        Map<String, Object> body = new HashMap<>();
        body.put("orderId", orderId);
        body.put("reason", reason != null ? reason : "Payment failed");
        String payload = objectMapper.writeValueAsString(body);
        rabbitTemplate.convertAndSend(config.getExchange(), config.getPaymentRejected(), payload);
        log.info("Published payment.rejected event orderId={} exchange={} routingKey={}",
                orderId, config.getExchange(), config.getPaymentRejected());
    }

    @SneakyThrows
    public void publishRefundCompleted(Long orderId) {
        String payload = objectMapper.writeValueAsString(Map.of("orderId", orderId));
        rabbitTemplate.convertAndSend(config.getExchange(), config.getRefundCompleted(), payload);
        log.info("Published refund.completed orderId={}", orderId);
    }

    @SneakyThrows
    public void publishRefundFailed(Long orderId, String reason) {
        Map<String, Object> body = new HashMap<>();
        body.put("orderId", orderId);
        body.put("reason", reason != null ? reason : "Refund failed");
        rabbitTemplate.convertAndSend(config.getExchange(), config.getRefundFailed(),
                objectMapper.writeValueAsString(body));
        log.info("Published refund.failed orderId={} reason={}", orderId, reason);
    }
}
