package com.logitrack.courierservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de RabbitMQ para courier-service.
 * Declara el exchange de envíos, la cola de consumo de estado y el convertidor JSON.
 */
@Configuration
public class RabbitMQConfig {

    public static final String SHIPMENT_EXCHANGE = "shipment.topic.exchange";
    public static final String COURIER_SHIPMENT_STATUS_QUEUE = "courier.shipment-status.queue";
    public static final String SHIPMENT_STATUS_ROUTING_KEY = "shipment.status.*";

    @Bean
    public TopicExchange shipmentTopicExchange() {
        return ExchangeBuilder.topicExchange(SHIPMENT_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue courierShipmentStatusQueue() {
        return QueueBuilder.durable(COURIER_SHIPMENT_STATUS_QUEUE)
                .build();
    }

    @Bean
    public Binding courierShipmentStatusBinding(Queue courierShipmentStatusQueue, TopicExchange shipmentTopicExchange) {
        return BindingBuilder.bind(courierShipmentStatusQueue)
                .to(shipmentTopicExchange)
                .with(SHIPMENT_STATUS_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
