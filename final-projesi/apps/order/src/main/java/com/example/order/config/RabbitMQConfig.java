package com.example.order.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-keys.order-created}")
    private String orderCreated;

    @Value("${rabbitmq.routing-keys.payment-accepted}")
    private String paymentAccepted;

    @Value("${rabbitmq.routing-keys.payment-rejected}")
    private String paymentRejected;

    @Value("${rabbitmq.routing-keys.shipping-processing}")
    private String shippingProcessing;

    @Value("${rabbitmq.routing-keys.shipping-delivered}")
    private String shippingDelivered;

    @Value("${rabbitmq.routing-keys.return-requested}")
    private String returnRequested;

    @Value("${rabbitmq.routing-keys.return-shipped}")
    private String returnShipped;

    @Value("${rabbitmq.routing-keys.return-delivered}")
    private String returnDelivered;

    @Value("${rabbitmq.routing-keys.refund-initiate}")
    private String refundInitiate;

    @Value("${rabbitmq.routing-keys.refund-completed}")
    private String refundCompleted;

    @Value("${rabbitmq.routing-keys.refund-failed}")
    private String refundFailed;

    @Value("${rabbitmq.queues.order-return-shipped}")
    private String orderReturnShippedQueue;

    @Value("${rabbitmq.queues.order-return-delivered}")
    private String orderReturnDeliveredQueue;

    @Value("${rabbitmq.queues.order-refund-completed}")
    private String orderRefundCompletedQueue;

    @Value("${rabbitmq.queues.order-refund-failed}")
    private String orderRefundFailedQueue;

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

    @Value("${rabbitmq.queues.order-payment-accepted}")
    private String orderPaymentAcceptedQueue;

    @Value("${rabbitmq.queues.order-payment-rejected}")
    private String orderPaymentRejectedQueue;

    @Value("${rabbitmq.queues.order-shipping-update}")
    private String orderShippingUpdateQueue;

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public Queue orderPaymentAcceptedQueue() {
        return new Queue(orderPaymentAcceptedQueue, true);
    }

    @Bean
    public Queue orderPaymentRejectedQueue() {
        return new Queue(orderPaymentRejectedQueue, true);
    }

    @Bean
    public Queue orderShippingUpdateQueue() {
        return new Queue(orderShippingUpdateQueue, true);
    }

    @Bean
    public Binding orderPaymentAcceptedBinding(Queue orderPaymentAcceptedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(orderPaymentAcceptedQueue).to(exchange).with(paymentAccepted);
    }

    @Bean
    public Binding orderPaymentRejectedBinding(Queue orderPaymentRejectedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(orderPaymentRejectedQueue).to(exchange).with(paymentRejected);
    }

    @Bean
    public Binding orderShippingProcessingBinding(Queue orderShippingUpdateQueue, TopicExchange exchange) {
        return BindingBuilder.bind(orderShippingUpdateQueue).to(exchange).with(shippingProcessing);
    }

    @Bean
    public Binding orderShippingDeliveredBinding(Queue orderShippingUpdateQueue, TopicExchange exchange) {
        return BindingBuilder.bind(orderShippingUpdateQueue).to(exchange).with(shippingDelivered);
    }

    @Bean
    public Queue orderReturnShippedQueue() {
        return new Queue(orderReturnShippedQueue, true);
    }

    @Bean
    public Queue orderReturnDeliveredQueue() {
        return new Queue(orderReturnDeliveredQueue, true);
    }

    @Bean
    public Queue orderRefundCompletedQueue() {
        return new Queue(orderRefundCompletedQueue, true);
    }

    @Bean
    public Queue orderRefundFailedQueue() {
        return new Queue(orderRefundFailedQueue, true);
    }

    @Bean
    public Binding orderReturnShippedBinding(Queue orderReturnShippedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(orderReturnShippedQueue).to(exchange).with(returnShipped);
    }

    @Bean
    public Binding orderReturnDeliveredBinding(Queue orderReturnDeliveredQueue, TopicExchange exchange) {
        return BindingBuilder.bind(orderReturnDeliveredQueue).to(exchange).with(returnDelivered);
    }

    @Bean
    public Binding orderRefundCompletedBinding(Queue orderRefundCompletedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(orderRefundCompletedQueue).to(exchange).with(refundCompleted);
    }

    @Bean
    public Binding orderRefundFailedBinding(Queue orderRefundFailedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(orderRefundFailedQueue).to(exchange).with(refundFailed);
    }

    public String getExchange()                       { return exchange; }
    public String getOrderCreated()                   { return orderCreated; }
    public String getOrderPaymentAcceptedQueue()      { return orderPaymentAcceptedQueue; }
    public String getOrderPaymentRejectedQueue()      { return orderPaymentRejectedQueue; }
    public String getOrderShippingUpdateQueue()       { return orderShippingUpdateQueue; }
    public String getNotificationOrderCreated()       { return notificationOrderCreated; }
    public String getNotificationPaymentAccepted()    { return notificationPaymentAccepted; }
    public String getNotificationPaymentRejected()    { return notificationPaymentRejected; }
    public String getNotificationOrderShipped()       { return notificationOrderShipped; }
    public String getNotificationOrderDelivered()     { return notificationOrderDelivered; }
    public String getNotificationReturnInitiated()    { return notificationReturnInitiated; }
    public String getNotificationReturnShipped()      { return notificationReturnShipped; }
    public String getNotificationRefunding()          { return notificationRefunding; }
    public String getNotificationReturned()           { return notificationReturned; }
    public String getNotificationReturnFailed()       { return notificationReturnFailed; }
    public String getReturnRequested()                { return returnRequested; }
    public String getRefundInitiate()                 { return refundInitiate; }
    public String getOrderReturnShippedQueue()        { return orderReturnShippedQueue; }
    public String getOrderReturnDeliveredQueue()      { return orderReturnDeliveredQueue; }
    public String getOrderRefundCompletedQueue()      { return orderRefundCompletedQueue; }
    public String getOrderRefundFailedQueue()         { return orderRefundFailedQueue; }
}
