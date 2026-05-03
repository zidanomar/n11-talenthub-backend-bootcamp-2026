package com.example.shipping.consumer;

import com.example.shipping.config.RabbitMQConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShippingConsumer {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQConfig config;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "${rabbitmq.queues.shipping}")
    @SneakyThrows
    public void handlePaymentAccepted(String message) {
        Map<?, ?> event = objectMapper.readValue(message, Map.class);
        Long orderId = ((Number) event.get("orderId")).longValue();
        String orderPayload = objectMapper.writeValueAsString(Map.of("orderId", orderId));

        log.info("Shipping started for orderId={}", orderId);

        Thread.sleep(randomDelay());
        publish(config.getShippingProcessing(), orderPayload);
        log.info("orderId={} -> PROCESSING", orderId);

        Thread.sleep(randomDelay());
        publish(config.getShippingDelivering(), orderPayload);
        log.info("orderId={} -> DELIVERING", orderId);

        Thread.sleep(randomDelay());
        publish(config.getShippingDelivered(), orderPayload);
        log.info("orderId={} -> DELIVERED", orderId);
    }

    @RabbitListener(queues = "#{rabbitMQConfig.shippingReturnQueue}")
    @SneakyThrows
    public void handleReturnRequested(String message) {
        Map<?, ?> event = objectMapper.readValue(message, Map.class);
        Long orderId = ((Number) event.get("orderId")).longValue();
        String orderPayload = objectMapper.writeValueAsString(Map.of("orderId", orderId));

        log.info("Return pickup started for orderId={}", orderId);

        Thread.sleep(randomDelay());
        publish(config.getReturnShipped(), orderPayload);
        log.info("orderId={} -> RETURN_SHIPPED", orderId);

        Thread.sleep(randomDelay());
        publish(config.getReturnDelivered(), orderPayload);
        log.info("orderId={} -> RETURN_DELIVERED (warehouse)", orderId);
    }

    private long randomDelay() {
        return 3000;
    }

    private void publish(String routingKey, String payload) {
        rabbitTemplate.convertAndSend(config.getExchange(), routingKey, payload);
    }
}
