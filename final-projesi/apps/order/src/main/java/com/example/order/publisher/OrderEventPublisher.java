package com.example.order.publisher;

import com.example.order.config.RabbitMQConfig;
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
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQConfig config;
    private final ObjectMapper objectMapper;

    public void publishOrderCreated(String payload) {
        rabbitTemplate.convertAndSend(config.getExchange(), config.getOrderCreated(), payload);
    }

    @SneakyThrows
    public void publishNotificationOrderCreated(Long orderId, String userId) {
        String payload = objectMapper.writeValueAsString(Map.of("orderId", orderId, "userId", userId));
        rabbitTemplate.convertAndSend(config.getExchange(), config.getNotificationOrderCreated(), payload);
        log.debug("Published notification.order.created orderId={} userId={}", orderId, userId);
    }

    @SneakyThrows
    public void publishNotificationPaymentAccepted(Long orderId, String userId) {
        String payload = objectMapper.writeValueAsString(Map.of("orderId", orderId, "userId", userId));
        rabbitTemplate.convertAndSend(config.getExchange(), config.getNotificationPaymentAccepted(), payload);
        log.debug("Published notification.payment.accepted orderId={} userId={}", orderId, userId);
    }

    @SneakyThrows
    public void publishNotificationPaymentRejected(Long orderId, String userId, String reason) {
        Map<String, Object> body = new HashMap<>();
        body.put("orderId", orderId);
        body.put("userId", userId);
        body.put("reason", reason != null ? reason : "Payment failed");
        rabbitTemplate.convertAndSend(config.getExchange(), config.getNotificationPaymentRejected(),
                objectMapper.writeValueAsString(body));
        log.debug("Published notification.payment.rejected orderId={} userId={}", orderId, userId);
    }

    @SneakyThrows
    public void publishNotificationOrderShipped(Long orderId, String userId) {
        String payload = objectMapper.writeValueAsString(Map.of("orderId", orderId, "userId", userId));
        rabbitTemplate.convertAndSend(config.getExchange(), config.getNotificationOrderShipped(), payload);
        log.debug("Published notification.order.shipped orderId={} userId={}", orderId, userId);
    }

    @SneakyThrows
    public void publishNotificationOrderDelivered(Long orderId, String userId) {
        String payload = objectMapper.writeValueAsString(Map.of("orderId", orderId, "userId", userId));
        rabbitTemplate.convertAndSend(config.getExchange(), config.getNotificationOrderDelivered(), payload);
        log.debug("Published notification.order.delivered orderId={} userId={}", orderId, userId);
    }

    @SneakyThrows
    public void publishReturnRequested(Long orderId) {
        String payload = objectMapper.writeValueAsString(Map.of("orderId", orderId));
        rabbitTemplate.convertAndSend(config.getExchange(), config.getReturnRequested(), payload);
        log.info("Published return.requested orderId={}", orderId);
    }

    @SneakyThrows
    public void publishRefundInitiate(Long orderId) {
        String payload = objectMapper.writeValueAsString(Map.of("orderId", orderId));
        rabbitTemplate.convertAndSend(config.getExchange(), config.getRefundInitiate(), payload);
        log.info("Published refund.initiate orderId={}", orderId);
    }

    @SneakyThrows
    public void publishNotificationReturnInitiated(Long orderId, String userId) {
        String payload = objectMapper.writeValueAsString(Map.of("orderId", orderId, "userId", userId));
        rabbitTemplate.convertAndSend(config.getExchange(), config.getNotificationReturnInitiated(), payload);
        log.debug("Published notification.return.initiated orderId={} userId={}", orderId, userId);
    }

    @SneakyThrows
    public void publishNotificationReturnShipped(Long orderId, String userId) {
        String payload = objectMapper.writeValueAsString(Map.of("orderId", orderId, "userId", userId));
        rabbitTemplate.convertAndSend(config.getExchange(), config.getNotificationReturnShipped(), payload);
        log.debug("Published notification.return.shipped orderId={} userId={}", orderId, userId);
    }

    @SneakyThrows
    public void publishNotificationRefunding(Long orderId, String userId) {
        String payload = objectMapper.writeValueAsString(Map.of("orderId", orderId, "userId", userId));
        rabbitTemplate.convertAndSend(config.getExchange(), config.getNotificationRefunding(), payload);
        log.debug("Published notification.refunding orderId={} userId={}", orderId, userId);
    }

    @SneakyThrows
    public void publishNotificationReturned(Long orderId, String userId) {
        String payload = objectMapper.writeValueAsString(Map.of("orderId", orderId, "userId", userId));
        rabbitTemplate.convertAndSend(config.getExchange(), config.getNotificationReturned(), payload);
        log.debug("Published notification.returned orderId={} userId={}", orderId, userId);
    }

    @SneakyThrows
    public void publishNotificationReturnFailed(Long orderId, String userId, String reason) {
        Map<String, Object> body = new HashMap<>();
        body.put("orderId", orderId);
        body.put("userId", userId);
        body.put("reason", reason != null ? reason : "Unknown error");
        rabbitTemplate.convertAndSend(config.getExchange(), config.getNotificationReturnFailed(),
                objectMapper.writeValueAsString(body));
        log.debug("Published notification.return.failed orderId={} userId={}", orderId, userId);
    }
}
