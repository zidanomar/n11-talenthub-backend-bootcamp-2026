package com.example.product.publisher;

import com.example.product.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQConfig config;

    public void publishPriceUpdated(String payload) {
        rabbitTemplate.convertAndSend(config.getExchange(), config.getProductPriceUpdated(), payload);
    }

    public void publishDeleted(String payload) {
        rabbitTemplate.convertAndSend(config.getExchange(), config.getProductDeleted(), payload);
    }
}
