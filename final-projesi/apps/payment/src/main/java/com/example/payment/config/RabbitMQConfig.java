package com.example.payment.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-keys.payment-accepted}")
    private String paymentAccepted;

    @Value("${rabbitmq.routing-keys.payment-rejected}")
    private String paymentRejected;

    @Value("${rabbitmq.routing-keys.refund-initiate}")
    private String refundInitiate;

    @Value("${rabbitmq.routing-keys.refund-completed}")
    private String refundCompleted;

    @Value("${rabbitmq.routing-keys.refund-failed}")
    private String refundFailed;

    @Value("${rabbitmq.queues.payment-refund-initiate}")
    private String paymentRefundInitiateQueue;

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public Queue paymentRefundInitiateQueue() {
        return new Queue(paymentRefundInitiateQueue, true);
    }

    @Bean
    public Binding paymentRefundInitiateBinding(Queue paymentRefundInitiateQueue, TopicExchange exchange) {
        return BindingBuilder.bind(paymentRefundInitiateQueue).to(exchange).with(refundInitiate);
    }

    public String getExchange()                    { return exchange; }
    public String getPaymentAccepted()             { return paymentAccepted; }
    public String getPaymentRejected()             { return paymentRejected; }
    public String getRefundCompleted()             { return refundCompleted; }
    public String getRefundFailed()                { return refundFailed; }
    public String getPaymentRefundInitiateQueue()  { return paymentRefundInitiateQueue; }
}
