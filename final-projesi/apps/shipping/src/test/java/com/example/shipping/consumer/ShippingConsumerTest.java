package com.example.shipping.consumer;

import com.example.shipping.config.RabbitMQConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShippingConsumerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private RabbitMQConfig config;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ShippingConsumer shippingConsumer;

    @Test
    void handlePaymentAccepted_publishesThreeShippingEvents() throws Exception {
        when(config.getExchange()).thenReturn("test.exchange");
        when(config.getShippingProcessing()).thenReturn("shipping.processing");
        when(config.getShippingDelivering()).thenReturn("shipping.delivering");
        when(config.getShippingDelivered()).thenReturn("shipping.delivered");

        shippingConsumer.handlePaymentAccepted("{\"orderId\":1}");

        verify(rabbitTemplate).convertAndSend("test.exchange", "shipping.processing", "{\"orderId\":1}");
        verify(rabbitTemplate).convertAndSend("test.exchange", "shipping.delivering", "{\"orderId\":1}");
        verify(rabbitTemplate).convertAndSend("test.exchange", "shipping.delivered", "{\"orderId\":1}");
    }

    @Test
    void handlePaymentAccepted_extractsOrderIdCorrectly() throws Exception {
        when(config.getExchange()).thenReturn("test.exchange");
        when(config.getShippingProcessing()).thenReturn("shipping.processing");
        when(config.getShippingDelivering()).thenReturn("shipping.delivering");
        when(config.getShippingDelivered()).thenReturn("shipping.delivered");

        shippingConsumer.handlePaymentAccepted("{\"orderId\":42}");

        verify(rabbitTemplate, times(3)).convertAndSend(
                eq("test.exchange"), anyString(), contains("42"));
    }
}
