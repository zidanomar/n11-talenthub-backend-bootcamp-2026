package com.example.notification.consumer;

import com.example.notification.config.RabbitMQConfig;
import com.example.notification.entity.Notification;
import com.example.notification.entity.NotificationType;
import com.example.notification.service.NotificationService;
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
public class NotificationConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "#{rabbitMQConfig.orderCreatedQueue}")
    @SneakyThrows
    public void onOrderCreated(String message) {
        log.info("notification.order.created received: {}", message);
        Map<String, Object> event = parseEvent(message);
        Long orderId = toLong(event.get("orderId"));
        String userId = (String) event.get("userId");

        notificationService.createAndPush(Notification.builder()
                .userId(userId)
                .type(NotificationType.ORDER_CREATED)
                .title("Order Placed")
                .message("Order #" + orderId + " was placed and is awaiting payment.")
                .orderId(orderId)
                .build());
    }

    @RabbitListener(queues = "#{rabbitMQConfig.paymentAcceptedQueue}")
    @SneakyThrows
    public void onPaymentAccepted(String message) {
        log.info("notification.payment.accepted received: {}", message);
        Map<String, Object> event = parseEvent(message);
        Long orderId = toLong(event.get("orderId"));
        String userId = (String) event.get("userId");

        notificationService.createAndPush(Notification.builder()
                .userId(userId)
                .type(NotificationType.PAYMENT_ACCEPTED)
                .title("Payment Confirmed")
                .message("Your payment for order #" + orderId + " was accepted.")
                .orderId(orderId)
                .build());
    }

    @RabbitListener(queues = "#{rabbitMQConfig.paymentRejectedQueue}")
    @SneakyThrows
    public void onPaymentRejected(String message) {
        log.info("notification.payment.rejected received: {}", message);
        Map<String, Object> event = parseEvent(message);
        Long orderId = toLong(event.get("orderId"));
        String userId = (String) event.get("userId");
        String reason = event.containsKey("reason") ? (String) event.get("reason") : "Payment failed";

        notificationService.createAndPush(Notification.builder()
                .userId(userId)
                .type(NotificationType.PAYMENT_REJECTED)
                .title("Payment Failed")
                .message("Payment for order #" + orderId + " failed: " + reason)
                .orderId(orderId)
                .build());
    }

    @RabbitListener(queues = "#{rabbitMQConfig.orderShippedQueue}")
    @SneakyThrows
    public void onOrderShipped(String message) {
        log.info("notification.order.shipped received: {}", message);
        Map<String, Object> event = parseEvent(message);
        Long orderId = toLong(event.get("orderId"));
        String userId = (String) event.get("userId");

        notificationService.createAndPush(Notification.builder()
                .userId(userId)
                .type(NotificationType.ORDER_SHIPPED)
                .title("Order Shipped")
                .message("Order #" + orderId + " is on its way!")
                .orderId(orderId)
                .build());
    }

    @RabbitListener(queues = "#{rabbitMQConfig.orderDeliveredQueue}")
    @SneakyThrows
    public void onOrderDelivered(String message) {
        log.info("notification.order.delivered received: {}", message);
        Map<String, Object> event = parseEvent(message);
        Long orderId = toLong(event.get("orderId"));
        String userId = (String) event.get("userId");

        notificationService.createAndPush(Notification.builder()
                .userId(userId)
                .type(NotificationType.ORDER_DELIVERED)
                .title("Order Delivered")
                .message("Order #" + orderId + " has been delivered.")
                .orderId(orderId)
                .build());
    }

    @RabbitListener(queues = "#{rabbitMQConfig.returnInitiatedQueue}")
    @SneakyThrows
    public void onReturnInitiated(String message) {
        log.info("notification.return.initiated received: {}", message);
        Map<String, Object> event = parseEvent(message);
        Long orderId = toLong(event.get("orderId"));
        String userId = (String) event.get("userId");

        notificationService.createAndPush(Notification.builder()
                .userId(userId)
                .type(NotificationType.RETURN_INITIATED)
                .title("Return Initiated")
                .message("Return for order #" + orderId + " has been initiated. Awaiting pickup.")
                .orderId(orderId)
                .build());
    }

    @RabbitListener(queues = "#{rabbitMQConfig.returnShippedQueue}")
    @SneakyThrows
    public void onReturnShipped(String message) {
        log.info("notification.return.shipped received: {}", message);
        Map<String, Object> event = parseEvent(message);
        Long orderId = toLong(event.get("orderId"));
        String userId = (String) event.get("userId");

        notificationService.createAndPush(Notification.builder()
                .userId(userId)
                .type(NotificationType.RETURN_SHIPPED)
                .title("Return Picked Up")
                .message("Your return for order #" + orderId + " has been picked up and is in transit.")
                .orderId(orderId)
                .build());
    }

    @RabbitListener(queues = "#{rabbitMQConfig.refundingQueue}")
    @SneakyThrows
    public void onRefunding(String message) {
        log.info("notification.refunding received: {}", message);
        Map<String, Object> event = parseEvent(message);
        Long orderId = toLong(event.get("orderId"));
        String userId = (String) event.get("userId");

        notificationService.createAndPush(Notification.builder()
                .userId(userId)
                .type(NotificationType.REFUNDING)
                .title("Refund Processing")
                .message("Returned package for order #" + orderId + " received. Refund is being processed.")
                .orderId(orderId)
                .build());
    }

    @RabbitListener(queues = "#{rabbitMQConfig.returnedQueue}")
    @SneakyThrows
    public void onReturned(String message) {
        log.info("notification.returned received: {}", message);
        Map<String, Object> event = parseEvent(message);
        Long orderId = toLong(event.get("orderId"));
        String userId = (String) event.get("userId");

        notificationService.createAndPush(Notification.builder()
                .userId(userId)
                .type(NotificationType.RETURNED)
                .title("Refund Completed")
                .message("Your refund for order #" + orderId + " has been completed.")
                .orderId(orderId)
                .build());
    }

    @RabbitListener(queues = "#{rabbitMQConfig.returnFailedQueue}")
    @SneakyThrows
    public void onReturnFailed(String message) {
        log.info("notification.return.failed received: {}", message);
        Map<String, Object> event = parseEvent(message);
        Long orderId = toLong(event.get("orderId"));
        String userId = (String) event.get("userId");
        String reason = event.containsKey("reason") ? (String) event.get("reason") : "Unknown error";

        notificationService.createAndPush(Notification.builder()
                .userId(userId)
                .type(NotificationType.RETURN_FAILED)
                .title("Refund Failed")
                .message("Refund for order #" + orderId + " failed: " + reason)
                .orderId(orderId)
                .build());
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseEvent(String message) {
        return objectMapper.readValue(message, Map.class);
    }

    private Long toLong(Object value) {
        if (value == null) throw new IllegalArgumentException("orderId missing in event payload");
        return ((Number) value).longValue();
    }
}
