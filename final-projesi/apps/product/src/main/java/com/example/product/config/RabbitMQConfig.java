package com.example.product.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-keys.product-price-updated}")
    private String productPriceUpdated;

    @Value("${rabbitmq.routing-keys.product-deleted}")
    private String productDeleted;

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchange, true, false);
    }

    public String getExchange()            { return exchange; }
    public String getProductPriceUpdated() { return productPriceUpdated; }
    public String getProductDeleted()      { return productDeleted; }
}
