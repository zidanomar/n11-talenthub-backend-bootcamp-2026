package com.example.order.consumer;

import com.example.order.entity.OrderStatus;
import com.example.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "#{rabbitMQConfig.orderPaymentAcceptedQueue}")
    @SneakyThrows
    public void handlePaymentAccepted(String message) {
        log.info("payment.accepted received: {}", message);
        Long orderId = extractOrderId(message);
        orderService.markPaid(orderId);
    }

    @RabbitListener(queues = "#{rabbitMQConfig.orderPaymentRejectedQueue}")
    @SneakyThrows
    public void handlePaymentRejected(String message) {
        log.info("payment.rejected received: {}", message);
        Map<String, Object> map = objectMapper.readValue(message, Map.class);
        Long orderId = ((Number) map.get("orderId")).longValue();
        String reason = map.containsKey("reason") ? (String) map.get("reason") : null;
        orderService.markPaymentFailed(orderId, reason);
    }

    @RabbitListener(queues = "#{rabbitMQConfig.orderShippingUpdateQueue}")
    @SneakyThrows
    public void handleShippingUpdate(String message,
                                     @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        log.info("shipping update [{}] received: {}", routingKey, message);
        Long orderId = extractOrderId(message);
        if (routingKey.equals("shipping.processing")) {
            orderService.updateStatus(orderId, OrderStatus.SHIPPED);
        } else if (routingKey.equals("shipping.delivered")) {
            orderService.updateStatus(orderId, OrderStatus.DELIVERED);
        }
    }

    @RabbitListener(queues = "#{rabbitMQConfig.orderReturnShippedQueue}")
    @SneakyThrows
    public void handleReturnShipped(String message) {
        log.info("return.shipped received: {}", message);
        orderService.markReturnShipped(extractOrderId(message));
    }

    @RabbitListener(queues = "#{rabbitMQConfig.orderReturnDeliveredQueue}")
    @SneakyThrows
    public void handleReturnDelivered(String message) {
        log.info("return.delivered received: {}", message);
        orderService.markRefunding(extractOrderId(message));
    }

    @RabbitListener(queues = "#{rabbitMQConfig.orderRefundCompletedQueue}")
    @SneakyThrows
    public void handleRefundCompleted(String message) {
        log.info("refund.completed received: {}", message);
        orderService.markReturned(extractOrderId(message));
    }

    @RabbitListener(queues = "#{rabbitMQConfig.orderRefundFailedQueue}")
    @SneakyThrows
    public void handleRefundFailed(String message) {
        log.info("refund.failed received: {}", message);
        Map<String, Object> map = objectMapper.readValue(message, Map.class);
        Long orderId = ((Number) map.get("orderId")).longValue();
        String reason = map.containsKey("reason") ? (String) map.get("reason") : null;
        orderService.markReturnFailed(orderId, reason);
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private Long extractOrderId(String message) {
        Map<String, Object> map = objectMapper.readValue(message, Map.class);
        return ((Number) map.get("orderId")).longValue();
    }
}
