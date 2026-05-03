package com.example.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-keys.notification-order-created}")
    private String notificationOrderCreated;

    @Value("${rabbitmq.routing-keys.notification-payment-accepted}")
    private String notificationPaymentAccepted;

    @Value("${rabbitmq.routing-keys.notification-payment-rejected}")
    private String notificationPaymentRejected;

    @Value("${rabbitmq.routing-keys.notification-order-shipped}")
    private String notificationOrderShipped;

    @Value("${rabbitmq.routing-keys.notification-order-delivered}")
    private String notificationOrderDelivered;

    @Value("${rabbitmq.routing-keys.notification-return-initiated}")
    private String notificationReturnInitiated;

    @Value("${rabbitmq.routing-keys.notification-return-shipped}")
    private String notificationReturnShipped;

    @Value("${rabbitmq.routing-keys.notification-refunding}")
    private String notificationRefunding;

    @Value("${rabbitmq.routing-keys.notification-returned}")
    private String notificationReturned;

    @Value("${rabbitmq.routing-keys.notification-return-failed}")
    private String notificationReturnFailed;

    @Value("${rabbitmq.notification-queues.order-created}")
    private String orderCreatedQueue;

    @Value("${rabbitmq.notification-queues.payment-accepted}")
    private String paymentAcceptedQueue;

    @Value("${rabbitmq.notification-queues.payment-rejected}")
    private String paymentRejectedQueue;

    @Value("${rabbitmq.notification-queues.order-shipped}")
    private String orderShippedQueue;

    @Value("${rabbitmq.notification-queues.order-delivered}")
    private String orderDeliveredQueue;

    @Value("${rabbitmq.notification-queues.return-initiated}")
    private String returnInitiatedQueue;

    @Value("${rabbitmq.notification-queues.return-shipped}")
    private String returnShippedQueue;

    @Value("${rabbitmq.notification-queues.refunding}")
    private String refundingQueue;

    @Value("${rabbitmq.notification-queues.returned}")
    private String returnedQueue;

    @Value("${rabbitmq.notification-queues.return-failed}")
    private String returnFailedQueue;

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public Queue notificationOrderCreatedQueue() {
        return new Queue(orderCreatedQueue, true);
    }

    @Bean
    public Queue notificationPaymentAcceptedQueue() {
        return new Queue(paymentAcceptedQueue, true);
    }

    @Bean
    public Queue notificationPaymentRejectedQueue() {
        return new Queue(paymentRejectedQueue, true);
    }

    @Bean
    public Queue notificationOrderShippedQueue() {
        return new Queue(orderShippedQueue, true);
    }

    @Bean
    public Queue notificationOrderDeliveredQueue() {
        return new Queue(orderDeliveredQueue, true);
    }

    @Bean
    public Queue notificationReturnInitiatedQueue() {
        return new Queue(returnInitiatedQueue, true);
    }

    @Bean
    public Queue notificationReturnShippedQueue() {
        return new Queue(returnShippedQueue, true);
    }

    @Bean
    public Queue notificationRefundingQueue() {
        return new Queue(refundingQueue, true);
    }

    @Bean
    public Queue notificationReturnedQueue() {
        return new Queue(returnedQueue, true);
    }

    @Bean
    public Queue notificationReturnFailedQueue() {
        return new Queue(returnFailedQueue, true);
    }

    @Bean
    public Binding notificationOrderCreatedBinding(Queue notificationOrderCreatedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(notificationOrderCreatedQueue).to(exchange).with(notificationOrderCreated);
    }

    @Bean
    public Binding notificationPaymentAcceptedBinding(Queue notificationPaymentAcceptedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(notificationPaymentAcceptedQueue).to(exchange).with(notificationPaymentAccepted);
    }

    @Bean
    public Binding notificationPaymentRejectedBinding(Queue notificationPaymentRejectedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(notificationPaymentRejectedQueue).to(exchange).with(notificationPaymentRejected);
    }

    @Bean
    public Binding notificationOrderShippedBinding(Queue notificationOrderShippedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(notificationOrderShippedQueue).to(exchange).with(notificationOrderShipped);
    }

    @Bean
    public Binding notificationOrderDeliveredBinding(Queue notificationOrderDeliveredQueue, TopicExchange exchange) {
        return BindingBuilder.bind(notificationOrderDeliveredQueue).to(exchange).with(notificationOrderDelivered);
    }

    @Bean
    public Binding notificationReturnInitiatedBinding(Queue notificationReturnInitiatedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(notificationReturnInitiatedQueue).to(exchange).with(notificationReturnInitiated);
    }

    @Bean
    public Binding notificationReturnShippedBinding(Queue notificationReturnShippedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(notificationReturnShippedQueue).to(exchange).with(notificationReturnShipped);
    }

    @Bean
    public Binding notificationRefundingBinding(Queue notificationRefundingQueue, TopicExchange exchange) {
        return BindingBuilder.bind(notificationRefundingQueue).to(exchange).with(notificationRefunding);
    }

    @Bean
    public Binding notificationReturnedBinding(Queue notificationReturnedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(notificationReturnedQueue).to(exchange).with(notificationReturned);
    }

    @Bean
    public Binding notificationReturnFailedBinding(Queue notificationReturnFailedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(notificationReturnFailedQueue).to(exchange).with(notificationReturnFailed);
    }

    public String getOrderCreatedQueue()     { return orderCreatedQueue; }
    public String getPaymentAcceptedQueue()  { return paymentAcceptedQueue; }
    public String getPaymentRejectedQueue()  { return paymentRejectedQueue; }
    public String getOrderShippedQueue()     { return orderShippedQueue; }
    public String getOrderDeliveredQueue()   { return orderDeliveredQueue; }
    public String getReturnInitiatedQueue()  { return returnInitiatedQueue; }
    public String getReturnShippedQueue()    { return returnShippedQueue; }
    public String getRefundingQueue()        { return refundingQueue; }
    public String getReturnedQueue()         { return returnedQueue; }
    public String getReturnFailedQueue()     { return returnFailedQueue; }
}
