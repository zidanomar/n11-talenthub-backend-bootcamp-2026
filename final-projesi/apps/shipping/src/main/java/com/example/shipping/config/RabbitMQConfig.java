package com.example.shipping.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.queues.shipping}")
    private String shippingQueue;

    @Value("${rabbitmq.routing-keys.payment-accepted}")
    private String paymentAccepted;

    @Value("${rabbitmq.routing-keys.shipping-processing}")
    private String shippingProcessing;

    @Value("${rabbitmq.routing-keys.shipping-delivering}")
    private String shippingDelivering;

    @Value("${rabbitmq.routing-keys.shipping-delivered}")
    private String shippingDelivered;

    @Value("${rabbitmq.queues.shipping-return}")
    private String shippingReturnQueue;

    @Value("${rabbitmq.routing-keys.return-requested}")
    private String returnRequested;

    @Value("${rabbitmq.routing-keys.return-shipped}")
    private String returnShipped;

    @Value("${rabbitmq.routing-keys.return-delivered}")
    private String returnDelivered;

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public Queue shippingQueue() {
        return new Queue(shippingQueue, true);
    }

    @Bean
    public Queue shippingReturnQueue() {
        return new Queue(shippingReturnQueue, true);
    }

    @Bean
    public Binding shippingBinding(Queue shippingQueue, TopicExchange exchange) {
        return BindingBuilder.bind(shippingQueue).to(exchange).with(paymentAccepted);
    }

    @Bean
    public Binding shippingReturnBinding(Queue shippingReturnQueue, TopicExchange exchange) {
        return BindingBuilder.bind(shippingReturnQueue).to(exchange).with(returnRequested);
    }

    public String getExchange()             { return exchange; }
    public String getShippingQueue()        { return shippingQueue; }
    public String getShippingProcessing()   { return shippingProcessing; }
    public String getShippingDelivering()   { return shippingDelivering; }
    public String getShippingDelivered()    { return shippingDelivered; }
    public String getShippingReturnQueue()  { return shippingReturnQueue; }
    public String getReturnShipped()        { return returnShipped; }
    public String getReturnDelivered()      { return returnDelivered; }
}
